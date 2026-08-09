package com.ghostsync.core;

/**
 * Stable normalized identity for one inventory/container slot.
 * Container epochs protect against vanilla reusing numeric container ids later.
 */
public record SlotKey(
        long connectionEpoch,
        long containerEpoch,
        int containerId,
        int slot) {

    public SlotKey {
        if (connectionEpoch < 0) {
            throw new IllegalArgumentException("connectionEpoch must be >= 0");
        }
        if (containerEpoch < 0) {
            throw new IllegalArgumentException("containerEpoch must be >= 0");
        }
        if (slot < 0) {
            throw new IllegalArgumentException("slot must be >= 0");
        }
    }
}
