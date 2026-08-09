package com.ghostsync.core;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Version-independent certainty engine for ghost detection.
 *
 * <p>A mismatch can become {@link SyncState#CONFIRMED_GHOST} only when the
 * client state was explicitly compared after Minecraft applied the same fresh
 * server evidence. Background observations and elapsed time can never confirm
 * a ghost.</p>
 */
public final class GhostTracker<K> {
    private final Map<K, Entry> entries = new HashMap<>();
    private final Set<K> confirmedGhosts = new HashSet<>();
    private long eventOrder;

    public synchronized void receiveAuthoritativeState(K key, Presence presence, long serverSequence) {
        receiveAuthoritativeEvidence(new AuthoritativeEvidence<>(key, presence, serverSequence));
    }

    public synchronized void receiveAuthoritativeEvidence(AuthoritativeEvidence<K> evidence) {
        Objects.requireNonNull(evidence, "evidence");

        Entry entry = entries.computeIfAbsent(evidence.key(), ignored -> new Entry());

        // A replayed or older server observation must never satisfy a client-side
        // pending barrier. Adapters therefore assign strictly increasing sequence ids.
        if (evidence.serverSequence() <= entry.authoritativeServerSequence) {
            return;
        }

        entry.authoritativePresence = evidence.presence();
        entry.authoritativeServerSequence = evidence.serverSequence();
        entry.authoritativeOrder = nextOrder();
        recomputeState(evidence.key(), entry);
    }

    /**
     * Records the client state after Minecraft has applied the authoritative
     * server update identified by {@code serverSequence}. This is the only
     * observation path that can make a mismatch a confirmed ghost.
     *
     * <p>If a newer server update has already replaced the referenced evidence,
     * the stale comparison is ignored completely.</p>
     */
    public synchronized void observeClientAfterAuthoritativeState(
            K key,
            Presence presence,
            long serverSequence) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(presence, "presence");

        Entry entry = entries.get(key);
        if (entry == null || entry.authoritativeServerSequence != serverSequence) {
            return;
        }

        entry.clientPresence = presence;
        entry.clientObservedOrder = nextOrder();
        entry.lastComparedServerSequence = serverSequence;
        recomputeState(key, entry);
    }

    /**
     * Records an unverified/background client observation.
     *
     * <p>If the visible client value changes into a mismatch through this path,
     * the tracker requires newer server evidence before any mismatch can be
     * confirmed. A change that merely returns the client to the last known
     * authoritative value can safely stop ghost rendering immediately.</p>
     */
    public synchronized void observeClientState(K key, Presence presence) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(presence, "presence");

        Entry entry = entries.computeIfAbsent(key, ignored -> new Entry());
        long order = nextOrder();

        if (entry.clientPresence == null || entry.clientPresence != presence) {
            boolean createsUnverifiedMismatch = entry.authoritativePresence == null
                    || entry.authoritativePresence != presence;
            if (createsUnverifiedMismatch) {
                entry.requiredAuthoritativeAfterOrder = Math.max(entry.requiredAuthoritativeAfterOrder, order);
                entry.lastComparedServerSequence = Long.MIN_VALUE;
            }
        }

        entry.clientPresence = presence;
        entry.clientObservedOrder = order;
        recomputeState(key, entry);
    }

    /**
     * Records a client-side mutation or prediction that happened without fresh
     * server confirmation. A newer authoritative event is required before this
     * key can become confirmed.
     */
    public synchronized void receiveClientMutation(K key, Presence presence) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(presence, "presence");

        Entry entry = entries.computeIfAbsent(key, ignored -> new Entry());
        long order = nextOrder();
        entry.clientPresence = presence;
        entry.clientObservedOrder = order;
        entry.requiredAuthoritativeAfterOrder = Math.max(entry.requiredAuthoritativeAfterOrder, order);
        entry.lastComparedServerSequence = Long.MIN_VALUE;
        recomputeState(key, entry);
    }

    /**
     * Marks an action as awaiting server confirmation even if the visible client
     * state has not changed yet.
     */
    public synchronized void markClientActionPending(K key) {
        Objects.requireNonNull(key, "key");

        Entry entry = entries.computeIfAbsent(key, ignored -> new Entry());
        long order = nextOrder();
        entry.requiredAuthoritativeAfterOrder = Math.max(entry.requiredAuthoritativeAfterOrder, order);
        entry.lastComparedServerSequence = Long.MIN_VALUE;
        recomputeState(key, entry);
    }

    public synchronized SyncState getState(K key) {
        Entry entry = entries.get(key);
        return entry == null ? SyncState.UNKNOWN : entry.state;
    }

    /**
     * Snapshot intended for a rendering cache. The returned set is detached from
     * the mutable tracker and contains only fully confirmed ghosts.
     */
    public synchronized Set<K> confirmedGhostKeys() {
        return Set.copyOf(confirmedGhosts);
    }

    public synchronized void invalidate(K key) {
        entries.remove(key);
        confirmedGhosts.remove(key);
    }

    /**
     * Invalidates a scope, such as all positions in an unloaded chunk or all
     * slots belonging to a closed container.
     */
    public synchronized int invalidateMatching(Predicate<K> predicate) {
        Objects.requireNonNull(predicate, "predicate");
        int before = entries.size();
        entries.keySet().removeIf(key -> {
            if (!predicate.test(key)) {
                return false;
            }
            confirmedGhosts.remove(key);
            return true;
        });
        return before - entries.size();
    }

    public synchronized void reset() {
        entries.clear();
        confirmedGhosts.clear();
        eventOrder = 0L;
    }

    public synchronized int trackedEntryCount() {
        return entries.size();
    }

    private void recomputeState(K key, Entry entry) {
        SyncState next = calculateState(entry);
        entry.state = next;
        if (next == SyncState.CONFIRMED_GHOST) {
            confirmedGhosts.add(key);
        } else {
            confirmedGhosts.remove(key);
        }
    }

    private SyncState calculateState(Entry entry) {
        if (entry.clientPresence == null || entry.authoritativePresence == null) {
            return SyncState.UNKNOWN;
        }

        if (entry.authoritativeOrder < entry.requiredAuthoritativeAfterOrder) {
            return SyncState.PENDING;
        }

        if (entry.clientPresence == entry.authoritativePresence) {
            return SyncState.MATCHED;
        }

        if (entry.clientPresence == Presence.PRESENT
                && entry.authoritativePresence == Presence.ABSENT) {
            if (entry.lastComparedServerSequence == entry.authoritativeServerSequence) {
                return SyncState.CONFIRMED_GHOST;
            }
            return SyncState.PENDING;
        }

        // Reverse ghosts (server present, client absent) are intentionally out of scope.
        return SyncState.UNKNOWN;
    }

    private long nextOrder() {
        if (eventOrder == Long.MAX_VALUE) {
            throw new IllegalStateException("Ghost tracker event sequence exhausted");
        }
        return ++eventOrder;
    }

    private static final class Entry {
        private Presence clientPresence;
        private long clientObservedOrder;
        private Presence authoritativePresence;
        private long authoritativeServerSequence = Long.MIN_VALUE;
        private long authoritativeOrder;
        private long requiredAuthoritativeAfterOrder;
        private long lastComparedServerSequence = Long.MIN_VALUE;
        private SyncState state = SyncState.UNKNOWN;
    }
}
