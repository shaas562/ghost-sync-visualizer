package com.ghostsync.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Replays realistic ordering/lifecycle scenarios without launching Minecraft.
 * These tests intentionally model only normalized evidence: packet classes stay
 * in the Fabric adapter and cannot affect detector certainty rules.
 */
class GhostReplayScenarioTest {
    @Test
    void delayedServerCorrectionConfirmsOnlyWhenPairedWithFreshEvidence() {
        GhostTracker<String> tracker = new GhostTracker<>();

        tracker.receiveAuthoritativeState("block", Presence.ABSENT, 1);
        tracker.observeClientAfterAuthoritativeState("block", Presence.ABSENT, 1);
        assertEquals(SyncState.MATCHED, tracker.getState("block"));

        tracker.receiveClientMutation("block", Presence.PRESENT);
        assertEquals(SyncState.PENDING, tracker.getState("block"));

        // An unrelated/newer observation does not pair itself automatically.
        tracker.receiveAuthoritativeState("block", Presence.ABSENT, 2);
        assertEquals(SyncState.PENDING, tracker.getState("block"));

        tracker.observeClientAfterAuthoritativeState("block", Presence.PRESENT, 2);
        assertEquals(SyncState.CONFIRMED_GHOST, tracker.getState("block"));
    }

    @Test
    void outOfOrderServerEvidenceCannotRollBackCertainty() {
        GhostTracker<String> tracker = new GhostTracker<>();

        tracker.receiveClientMutation("slot", Presence.PRESENT);
        tracker.receiveAuthoritativeState("slot", Presence.ABSENT, 20);
        tracker.observeClientAfterAuthoritativeState("slot", Presence.PRESENT, 20);
        assertEquals(SyncState.CONFIRMED_GHOST, tracker.getState("slot"));

        tracker.receiveAuthoritativeState("slot", Presence.PRESENT, 19);
        tracker.observeClientAfterAuthoritativeState("slot", Presence.PRESENT, 19);
        assertEquals(SyncState.CONFIRMED_GHOST, tracker.getState("slot"));
    }

    @Test
    void newLocalPredictionRevokesExistingConfirmationUntilNewServerEvidence() {
        GhostTracker<String> tracker = new GhostTracker<>();

        tracker.receiveAuthoritativeState("block", Presence.ABSENT, 5);
        tracker.observeClientAfterAuthoritativeState("block", Presence.PRESENT, 5);
        assertEquals(SyncState.CONFIRMED_GHOST, tracker.getState("block"));

        tracker.markClientActionPending("block");
        assertEquals(SyncState.PENDING, tracker.getState("block"));

        tracker.observeClientState("block", Presence.PRESENT);
        assertEquals(SyncState.PENDING, tracker.getState("block"));

        tracker.receiveAuthoritativeState("block", Presence.ABSENT, 6);
        tracker.observeClientAfterAuthoritativeState("block", Presence.PRESENT, 6);
        assertEquals(SyncState.CONFIRMED_GHOST, tracker.getState("block"));
    }

    @Test
    void disconnectClearsAllCertaintyAndReusedCoordinatesNeedFreshEvidence() {
        GhostDetectionCore core = new GhostDetectionCore();
        BlockKey oldBlock = new BlockKey(1, 1, "minecraft:overworld", 12, 64, 12);
        SlotKey oldSlot = new SlotKey(1, 4, 7, 3);

        confirm(core.blocks(), oldBlock, 1);
        confirm(core.slots(), oldSlot, 1);
        assertTrue(core.confirmedGhostBlocks().contains(oldBlock));
        assertTrue(core.confirmedGhostSlots().contains(oldSlot));

        core.resetConnectionState();
        assertTrue(core.confirmedGhostBlocks().isEmpty());
        assertTrue(core.confirmedGhostSlots().isEmpty());

        BlockKey reusedCoordinate = new BlockKey(2, 1, "minecraft:overworld", 12, 64, 12);
        core.blocks().observeClientState(reusedCoordinate, Presence.PRESENT);
        assertEquals(SyncState.UNKNOWN, core.blocks().getState(reusedCoordinate));
        assertFalse(core.confirmedGhostBlocks().contains(reusedCoordinate));
    }

    @Test
    void containerIdReuseAcrossEpochsCannotLeakCursorOrSlotGhosts() {
        GhostDetectionCore core = new GhostDetectionCore();
        SlotKey oldSlot = new SlotKey(3, 100, 8, 4);
        SlotKey oldCursor = new SlotKey(3, 100, 8, Integer.MAX_VALUE);
        SlotKey newSlot = new SlotKey(3, 101, 8, 4);

        confirm(core.slots(), oldSlot, 1);
        confirm(core.slots(), oldCursor, 2);
        assertEquals(2, core.closeContainer(3, 100, 8));

        core.slots().observeClientState(newSlot, Presence.PRESENT);
        assertEquals(SyncState.UNKNOWN, core.slots().getState(newSlot));
        assertTrue(core.confirmedGhostSlots().isEmpty());
    }

    @Test
    void chunkUnloadDropsGhostBeforeSameCoordinatesCanBeReused() {
        GhostDetectionCore core = new GhostDetectionCore();
        BlockKey key = new BlockKey(9, 2, "minecraft:the_nether", 32, 70, -1);
        confirm(core.blocks(), key, 1);
        assertTrue(core.confirmedGhostBlocks().contains(key));

        assertEquals(1, core.unloadChunk(9, 2, "minecraft:the_nether", 2, -1));
        assertEquals(SyncState.UNKNOWN, core.blocks().getState(key));
        assertTrue(core.confirmedGhostBlocks().isEmpty());
    }

    private static <K> void confirm(GhostTracker<K> tracker, K key, long sequence) {
        tracker.receiveAuthoritativeState(key, Presence.ABSENT, sequence);
        tracker.observeClientAfterAuthoritativeState(key, Presence.PRESENT, sequence);
    }
}
