package com.ghostsync.core;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Version-independent certainty engine for ghost detection.
 *
 * <p>The tracker deliberately treats local mutations as pending until a newer
 * authoritative observation arrives. Time alone can never confirm a ghost.</p>
 */
public final class GhostTracker<K> {
    private final Map<K, Entry> entries = new HashMap<>();
    private long eventOrder;

    public synchronized void receiveAuthoritativeState(K key, Presence presence, long revision) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(presence, "presence");

        Entry entry = entries.computeIfAbsent(key, ignored -> new Entry());
        long order = nextOrder();

        if (revision < entry.authoritativeRevision) {
            return;
        }

        entry.authoritativePresence = presence;
        entry.authoritativeRevision = revision;
        entry.authoritativeOrder = order;
    }

    /**
     * Records the client state as an observation, typically after Minecraft has
     * applied an incoming authoritative packet. This does not create a new
     * freshness requirement by itself.
     */
    public synchronized void observeClientState(K key, Presence presence) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(presence, "presence");

        Entry entry = entries.computeIfAbsent(key, ignored -> new Entry());
        entry.clientPresence = presence;
        entry.clientObservedOrder = nextOrder();
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

        if (entry.clientPresence == Presence.PRESENT && entry.authoritativePresence == Presence.ABSENT) {
            return SyncState.CONFIRMED_GHOST;
        }

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
        private long authoritativeRevision = Long.MIN_VALUE;
        private long authoritativeOrder;
        private long requiredAuthoritativeAfterOrder;
    }
}
