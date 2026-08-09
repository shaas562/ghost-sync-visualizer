package com.ghostsync.core;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

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
    }

    /**
     * Records an unverified/background client observation.
     *
     * <p>If the visible client value changes through this path, the tracker
     * requires newer server evidence before any mismatch can be confirmed. This
     * is intentionally conservative because the observation may have happened
     * while a legitimate server update is still in flight.</p>
     */
    public synchronized void observeClientState(K key, Presence presence) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(presence, "presence");

        Entry entry = entries.computeIfAbsent(key, ignored -> new Entry());
        long order = nextOrder();

        if (entry.clientPresence == null || entry.clientPresence != presence) {
            entry.requiredAuthoritativeAfterOrder = Math.max(entry.requiredAuthoritativeAfterOrder, order);
            entry.lastComparedServerSequence = Long.MIN_VALUE;
        }

        entry.clientPresence = presence;
        entry.clientObservedOrder = order;
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
    }

    public synchronized SyncState getState(K key) {
        Entry entry = entries.get(key);
        if (entry == null || entry.clientPresence == null || entry.authoritativePresence == null) {
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

    public synchronized void invalidate(K key) {
        entries.remove(key);
    }

    public synchronized void reset() {
        entries.clear();
        eventOrder = 0L;
    }

    public synchronized int trackedEntryCount() {
        return entries.size();
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
    }
}
