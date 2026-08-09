package com.ghostsync.fabric.client;

import com.ghostsync.core.GhostDetectionCore;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Shared client runtime state. Minecraft-specific adapters write normalized
 * evidence into {@link #DETECTION}; renderers only read confirmed snapshots.
 */
public final class GhostSyncRuntime {
    public static final GhostDetectionCore DETECTION = new GhostDetectionCore();

    private static final AtomicLong REFRESH_GENERATION = new AtomicLong();

    private GhostSyncRuntime() {
    }

    /**
     * Requests re-evaluation against currently known trustworthy server state.
     * This deliberately does not claim to force a vanilla server to resend data.
     */
    public static long requestRefresh() {
        return REFRESH_GENERATION.incrementAndGet();
    }

    public static long refreshGeneration() {
        return REFRESH_GENERATION.get();
    }
}
