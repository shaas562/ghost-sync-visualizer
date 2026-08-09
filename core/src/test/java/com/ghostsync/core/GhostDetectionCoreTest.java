package com.ghostsync.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class GhostDetectionCoreTest {
    @Test
    void chunkUnloadDropsOnlyThatChunk() {
        GhostDetectionCore core = new GhostDetectionCore();
        BlockKey firstChunk = new BlockKey(2, 4, "minecraft:overworld", 15, 70, 1);
        BlockKey secondChunk = new BlockKey(2, 4, "minecraft:overworld", 16, 70, 1);

        confirm(core.blocks(), firstChunk, 1);
        confirm(core.blocks(), secondChunk, 2);

        assertEquals(1, core.unloadChunk(2, 4, "minecraft:overworld", 0, 0));
        assertEquals(SyncState.UNKNOWN, core.blocks().getState(firstChunk));
        assertEquals(SyncState.CONFIRMED_GHOST, core.blocks().getState(secondChunk));
    }

    @Test
    void closingContainerDoesNotDropReusedIdFromDifferentEpoch() {
        GhostDetectionCore core = new GhostDetectionCore();
        SlotKey oldContainer = new SlotKey(3, 10, 5, 2);
        SlotKey newContainer = new SlotKey(3, 11, 5, 2);

        confirm(core.slots(), oldContainer, 1);
        confirm(core.slots(), newContainer, 2);

        assertEquals(1, core.closeContainer(3, 10, 5));
        assertEquals(SyncState.UNKNOWN, core.slots().getState(oldContainer));
        assertEquals(SyncState.CONFIRMED_GHOST, core.slots().getState(newContainer));
    }

    @Test
    void closingContainerNeverDropsConnectionScopedPlayerInventory() {
        GhostDetectionCore core = new GhostDetectionCore();
        SlotKey containerSlot = new SlotKey(3, 10, 5, 2);
        SlotKey playerInventorySlot = new SlotKey(3, 0, -1, 8);

        confirm(core.slots(), containerSlot, 1);
        confirm(core.slots(), playerInventorySlot, 2);

        assertEquals(1, core.closeContainer(3, 10, 5));
        assertEquals(SyncState.UNKNOWN, core.slots().getState(containerSlot));
        assertEquals(SyncState.CONFIRMED_GHOST, core.slots().getState(playerInventorySlot));
    }

    @Test
    void individualDetectorResetsDoNotCrossClear() {
        GhostDetectionCore core = new GhostDetectionCore();
        BlockKey block = new BlockKey(1, 1, "minecraft:overworld", 0, 64, 0);
        SlotKey slot = new SlotKey(1, 0, -1, 3);

        confirm(core.blocks(), block, 1);
        confirm(core.slots(), slot, 1);

        core.resetBlockState();
        assertEquals(SyncState.UNKNOWN, core.blocks().getState(block));
        assertEquals(SyncState.CONFIRMED_GHOST, core.slots().getState(slot));

        confirm(core.blocks(), block, 2);
        core.resetSlotState();
        assertEquals(SyncState.CONFIRMED_GHOST, core.blocks().getState(block));
        assertEquals(SyncState.UNKNOWN, core.slots().getState(slot));
    }

    @Test
    void worldResetKeepsSlotTrackingButConnectionResetClearsEverything() {
        GhostDetectionCore core = new GhostDetectionCore();
        BlockKey block = new BlockKey(1, 1, "minecraft:overworld", 0, 64, 0);
        SlotKey slot = new SlotKey(1, 0, -1, 3);

        confirm(core.blocks(), block, 1);
        confirm(core.slots(), slot, 1);

        core.resetWorldState();
        assertEquals(SyncState.UNKNOWN, core.blocks().getState(block));
        assertEquals(SyncState.CONFIRMED_GHOST, core.slots().getState(slot));

        core.resetConnectionState();
        assertEquals(SyncState.UNKNOWN, core.slots().getState(slot));
    }

    private static <K> void confirm(GhostTracker<K> tracker, K key, long sequence) {
        tracker.receiveAuthoritativeState(key, Presence.ABSENT, sequence);
        tracker.observeClientAfterAuthoritativeState(key, Presence.PRESENT, sequence);
    }
}
