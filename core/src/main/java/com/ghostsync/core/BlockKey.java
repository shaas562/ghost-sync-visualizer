package com.ghostsync.core;

import java.util.Objects;

/**
 * Stable normalized identity for one block position within a specific connection/world epoch.
 * Epochs prevent stale state from being reused after reconnects or dimension reloads.
 */
public record BlockKey(
        long connectionEpoch,
        long worldEpoch,
        String dimensionId,
        int x,
        int y,
        int z) {

    public BlockKey {
        if (connectionEpoch < 0) {
            throw new IllegalArgumentException("connectionEpoch must be >= 0");
        }
        if (worldEpoch < 0) {
            throw new IllegalArgumentException("worldEpoch must be >= 0");
        }
        Objects.requireNonNull(dimensionId, "dimensionId");
        if (dimensionId.isBlank()) {
            throw new IllegalArgumentException("dimensionId must not be blank");
        }
    }
}
