package com.ghostsync.core;

import java.util.Set;

/**
 * Version-independent facade that keeps block and item detection isolated while
 * exposing the small snapshots later needed by rendering.
 */
public final class GhostDetectionCore {
    private final GhostTracker<BlockKey> blocks = new GhostTracker<>();
    private final GhostTracker<SlotKey> slots = new GhostTracker<>();

    public GhostTracker<BlockKey> blocks() {
        return blocks;
    }

    public GhostTracker<SlotKey> slots() {
        return slots;
    }

    public Set<BlockKey> confirmedGhostBlocks() {
        return blocks.confirmedGhostKeys();
    }

    public Set<SlotKey> confirmedGhostSlots() {
        return slots.confirmedGhostKeys();
    }

    public int unloadChunk(
            long connectionEpoch,
            long worldEpoch,
            String dimensionId,
            int chunkX,
            int chunkZ) {
        return blocks.invalidateMatching(key ->
                key.connectionEpoch() == connectionEpoch
                        && key.worldEpoch() == worldEpoch
                        && key.dimensionId().equals(dimensionId)
                        && Math.floorDiv(key.x(), 16) == chunkX
                        && Math.floorDiv(key.z(), 16) == chunkZ);
    }

    public int closeContainer(
            long connectionEpoch,
            long containerEpoch,
            int containerId) {
        return slots.invalidateMatching(key ->
                key.connectionEpoch() == connectionEpoch
                        && key.containerEpoch() == containerEpoch
                        && key.containerId() == containerId);
    }

    /** Clears only block certainty; item certainty is intentionally untouched. */
    public void resetBlockState() {
        blocks.reset();
    }

    /** Clears only item/slot certainty; block certainty is intentionally untouched. */
    public void resetSlotState() {
        slots.reset();
    }

    public void resetWorldState() {
        resetBlockState();
    }

    public void resetConnectionState() {
        resetBlockState();
        resetSlotState();
    }
}
