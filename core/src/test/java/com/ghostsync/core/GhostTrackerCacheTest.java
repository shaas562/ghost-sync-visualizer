package com.ghostsync.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.junit.jupiter.api.Test;

class GhostTrackerCacheTest {
    @Test
    void confirmedGhostSnapshotTracksStateChanges() {
        GhostTracker<BlockKey> tracker = new GhostTracker<>();
        BlockKey key = new BlockKey(1, 1, "minecraft:overworld", 10, 64, 10);

        tracker.receiveAuthoritativeState(key, Presence.ABSENT, 1);
        tracker.observeClientAfterAuthoritativeState(key, Presence.PRESENT, 1);

        assertEquals(Set.of(key), tracker.confirmedGhostKeys());

        tracker.observeClientState(key, Presence.ABSENT);

        assertTrue(tracker.confirmedGhostKeys().isEmpty());
        assertEquals(SyncState.MATCHED, tracker.getState(key));
    }

    @Test
    void pendingStateNeverEntersConfirmedGhostSnapshot() {
        GhostTracker<SlotKey> tracker = new GhostTracker<>();
        SlotKey key = new SlotKey(1, 1, 4, 8);

        tracker.receiveAuthoritativeState(key, Presence.ABSENT, 1);
        tracker.observeClientAfterAuthoritativeState(key, Presence.ABSENT, 1);
        tracker.receiveClientMutation(key, Presence.PRESENT);

        assertEquals(SyncState.PENDING, tracker.getState(key));
        assertFalse(tracker.confirmedGhostKeys().contains(key));
    }

    @Test
    void scopedInvalidationCanDropOneChunkWithoutTouchingAnother() {
        GhostTracker<BlockKey> tracker = new GhostTracker<>();
        BlockKey chunkZero = new BlockKey(1, 1, "minecraft:overworld", 1, 64, 1);
        BlockKey chunkOne = new BlockKey(1, 1, "minecraft:overworld", 17, 64, 1);

        confirmGhost(tracker, chunkZero, 1);
        confirmGhost(tracker, chunkOne, 2);

        int removed = tracker.invalidateMatching(key -> Math.floorDiv(key.x(), 16) == 0);

        assertEquals(1, removed);
        assertEquals(SyncState.UNKNOWN, tracker.getState(chunkZero));
        assertEquals(SyncState.CONFIRMED_GHOST, tracker.getState(chunkOne));
        assertEquals(Set.of(chunkOne), tracker.confirmedGhostKeys());
    }

    private static void confirmGhost(GhostTracker<BlockKey> tracker, BlockKey key, long sequence) {
        tracker.receiveAuthoritativeState(key, Presence.ABSENT, sequence);
        tracker.observeClientAfterAuthoritativeState(key, Presence.PRESENT, sequence);
        assertTrue(tracker.confirmedGhostKeys().contains(key));
    }
}
