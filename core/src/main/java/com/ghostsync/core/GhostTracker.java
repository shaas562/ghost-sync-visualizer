package com.ghostsync.core;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

/** Strict, version-independent ghost certainty engine. */
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
        if (evidence.serverSequence() <= entry.authoritativeServerSequence) return;
        entry.authoritativePresence = evidence.presence();
        entry.authoritativeServerSequence = evidence.serverSequence();
        entry.authoritativeOrder = nextOrder();
        recomputeState(evidence.key(), entry);
    }

    public synchronized void observeClientAfterAuthoritativeState(K key, Presence presence, long serverSequence) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(presence, "presence");
        Entry entry = entries.get(key);
        if (entry == null || entry.authoritativeServerSequence != serverSequence) return;
        entry.clientPresence = presence;
        entry.clientObservedOrder = nextOrder();
        entry.lastComparedServerSequence = serverSequence;
        recomputeState(key, entry);
    }

    public synchronized void observeClientState(K key, Presence presence) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(presence, "presence");
        Entry existing = entries.get(key);
        if (existing != null && existing.clientPresence == presence) return;
        Entry entry = existing != null ? existing : entries.computeIfAbsent(key, ignored -> new Entry());
        long order = nextOrder();
        if (entry.authoritativePresence == null || entry.authoritativePresence != presence) {
            entry.requiredAuthoritativeAfterOrder = Math.max(entry.requiredAuthoritativeAfterOrder, order);
            entry.lastComparedServerSequence = Long.MIN_VALUE;
        }
        entry.clientPresence = presence;
        entry.clientObservedOrder = order;
        recomputeState(key, entry);
    }

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

    public synchronized Set<K> confirmedGhostKeys() { return Set.copyOf(confirmedGhosts); }

    public synchronized void invalidate(K key) {
        entries.remove(key);
        confirmedGhosts.remove(key);
    }

    public synchronized int invalidateMatching(Predicate<K> predicate) {
        Objects.requireNonNull(predicate, "predicate");
        int before = entries.size();
        entries.keySet().removeIf(key -> {
            if (!predicate.test(key)) return false;
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

    public synchronized int trackedEntryCount() { return entries.size(); }

    private void recomputeState(K key, Entry entry) {
        entry.state = calculateState(entry);
        if (entry.state == SyncState.CONFIRMED_GHOST) confirmedGhosts.add(key);
        else confirmedGhosts.remove(key);
    }

    private SyncState calculateState(Entry entry) {
        if (entry.clientPresence == null || entry.authoritativePresence == null) return SyncState.UNKNOWN;
        if (entry.authoritativeOrder < entry.requiredAuthoritativeAfterOrder) return SyncState.PENDING;
        if (entry.clientPresence == entry.authoritativePresence) return SyncState.MATCHED;
        if (entry.clientPresence == Presence.PRESENT && entry.authoritativePresence == Presence.ABSENT) {
            return entry.lastComparedServerSequence == entry.authoritativeServerSequence
                    ? SyncState.CONFIRMED_GHOST : SyncState.PENDING;
        }
        return SyncState.UNKNOWN;
    }

    private long nextOrder() {
        if (eventOrder == Long.MAX_VALUE) throw new IllegalStateException("Ghost tracker event sequence exhausted");
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
