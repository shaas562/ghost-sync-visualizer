package com.ghostsync.fabric.client.config;

/** Persisted user configuration. Values are normalized after every load. */
public final class GhostSyncConfig {
    public static final int CURRENT_SCHEMA_VERSION = 1;

    public int schemaVersion = CURRENT_SCHEMA_VERSION;
    public boolean enabled = true;
    public BlockSettings blocks = new BlockSettings();
    public ItemSettings items = new ItemSettings();
    public AdvancedSettings advanced = new AdvancedSettings();

    public void normalize() {
        schemaVersion = CURRENT_SCHEMA_VERSION;
        if (blocks == null) {
            blocks = new BlockSettings();
        }
        if (items == null) {
            items = new ItemSettings();
        }
        if (advanced == null) {
            advanced = new AdvancedSettings();
        }
        blocks.overlayStrength = clamp01(blocks.overlayStrength);
        blocks.transparencyStrength = clamp01(blocks.transparencyStrength);
        blocks.detectionDistanceChunks = clamp(blocks.detectionDistanceChunks, 2, 32);
        blocks.performanceLevel = clamp(blocks.performanceLevel, 1, 5);
        items.overlayStrength = clamp01(items.overlayStrength);
        items.transparencyStrength = clamp01(items.transparencyStrength);
    }

    public boolean shouldDetectBlocks() {
        return enabled && blocks.enabled
                && (blocks.overlayStrength > 0.0 || blocks.transparencyStrength > 0.0);
    }

    public boolean shouldDetectItems() {
        return enabled && items.enabled
                && (items.overlayStrength > 0.0 || items.transparencyStrength > 0.0);
    }

    private static double clamp01(double value) {
        if (!Double.isFinite(value)) {
            return 0.0;
        }
        return Math.max(0.0, Math.min(1.0, value));
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public static final class BlockSettings {
        public boolean enabled = true;
        public double overlayStrength = 0.45;
        public double transparencyStrength = 0.20;
        public int detectionDistanceChunks = 12;
        public int performanceLevel = 3;
    }

    public static final class ItemSettings {
        public boolean enabled = true;
        public double overlayStrength = 0.45;
        public double transparencyStrength = 0.20;
    }

    public static final class AdvancedSettings {
        public boolean loggingEnabled = false;
    }
}
