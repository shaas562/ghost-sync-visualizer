package com.ghostsync.fabric.client;

import com.ghostsync.core.GhostDetectionCore;
import com.ghostsync.core.ServerSequenceCounter;
import com.ghostsync.core.SlotKey;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Shared client runtime state. Minecraft-specific adapters write normalized
 * evidence into {@link #DETECTION}; renderers only read confirmed snapshots.
 */
public final class GhostSyncRuntime {
    public static final GhostDetectionCore DETECTION = new GhostDetectionCore();
    public static final long PLAYER_INVENTORY_EPOCH = 0L;
    public static final int PLAYER_INVENTORY_CONTAINER_ID = -1;

    private static final AtomicLong REFRESH_GENERATION = new AtomicLong();
    private static final Map<Object, Long> CONTAINER_EPOCHS = new IdentityHashMap<>();

    private static ServerSequenceCounter serverSequence = new ServerSequenceCounter();
    private static long connectionEpoch;
    private static long worldEpoch;
    private static long nextContainerEpoch;

    private GhostSyncRuntime() {
    }

    public static synchronized void beginConnection() {
        connectionEpoch = incrementEpoch(connectionEpoch, "connection");
        worldEpoch = 0;
        nextContainerEpoch = 0;
        CONTAINER_EPOCHS.clear();
        serverSequence = new ServerSequenceCounter();
        DETECTION.resetConnectionState();
        requestRefresh();
    }

    public static synchronized void beginWorld() {
        worldEpoch = incrementEpoch(worldEpoch, "world");
        DETECTION.resetWorldState();
        requestRefresh();
    }

    public static synchronized void endConnection() {
        CONTAINER_EPOCHS.clear();
        DETECTION.resetConnectionState();
        requestRefresh();
    }

    public static synchronized long connectionEpoch() {
        return connectionEpoch;
    }

    public static synchronized long worldEpoch() {
        return worldEpoch;
    }

    public static synchronized long nextServerSequence() {
        return serverSequence.next();
    }

    public static synchronized long containerEpoch(Object menuIdentity) {
        return CONTAINER_EPOCHS.computeIfAbsent(menuIdentity, ignored -> {
            nextContainerEpoch = incrementEpoch(nextContainerEpoch, "container");
            return nextContainerEpoch;
        });
    }

    public static synchronized void forgetContainer(Object menuIdentity) {
        CONTAINER_EPOCHS.remove(menuIdentity);
    }

    /** Player inventory identity is connection-scoped and never reuses a menu id. */
    public static SlotKey playerInventoryKey(int slot) {
        return new SlotKey(
                connectionEpoch(),
                PLAYER_INVENTORY_EPOCH,
                PLAYER_INVENTORY_CONTAINER_ID,
                slot);
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

    private static long incrementEpoch(long value, String name) {
        if (value == Long.MAX_VALUE) {
            throw new IllegalStateException(name + " epoch exhausted");
        }
        return value + 1;
    }
}
