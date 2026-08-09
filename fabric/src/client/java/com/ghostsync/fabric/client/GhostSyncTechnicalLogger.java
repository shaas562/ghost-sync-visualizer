package com.ghostsync.fabric.client;

import com.ghostsync.core.BlockKey;
import com.ghostsync.core.SlotKey;
import com.ghostsync.fabric.client.config.GhostSyncConfigManager;
import java.util.Set;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Optional low-volume diagnostics for confirmed-set transitions. */
public final class GhostSyncTechnicalLogger {
    private static final Logger LOGGER = LoggerFactory.getLogger("GhostSync");
    private static Set<BlockKey> previousBlocks = Set.of();
    private static Set<SlotKey> previousSlots = Set.of();
    private static boolean loggingWasEnabled;
    private static boolean initialized;

    private GhostSyncTechnicalLogger() {}

    public static synchronized void initialize() {
        if (initialized) return;
        initialized = true;
        ClientTickEvents.END_CLIENT_TICK.register(client -> sample());
    }

    private static void sample() {
        if (!GhostSyncConfigManager.current().advanced.loggingEnabled) {
            previousBlocks = Set.of();
            previousSlots = Set.of();
            loggingWasEnabled = false;
            return;
        }

        Set<BlockKey> currentBlocks = GhostSyncRuntime.DETECTION.confirmedGhostBlocks();
        Set<SlotKey> currentSlots = GhostSyncRuntime.DETECTION.confirmedGhostSlots();

        if (!loggingWasEnabled) {
            previousBlocks = currentBlocks;
            previousSlots = currentSlots;
            loggingWasEnabled = true;
            LOGGER.info(
                    "Technical logging enabled; {} confirmed block ghosts and {} confirmed item ghosts already tracked",
                    currentBlocks.size(),
                    currentSlots.size());
            return;
        }

        logBlockChanges(previousBlocks, currentBlocks);
        logSlotChanges(previousSlots, currentSlots);
        previousBlocks = currentBlocks;
        previousSlots = currentSlots;
    }

    private static void logBlockChanges(Set<BlockKey> previous, Set<BlockKey> current) {
        for (BlockKey key : current) {
            if (!previous.contains(key)) LOGGER.info("Confirmed ghost block: {}", key);
        }
        for (BlockKey key : previous) {
            if (!current.contains(key)) LOGGER.info("Cleared ghost block: {}", key);
        }
    }

    private static void logSlotChanges(Set<SlotKey> previous, Set<SlotKey> current) {
        for (SlotKey key : current) {
            if (!previous.contains(key)) LOGGER.info("Confirmed ghost item: {}", key);
        }
        for (SlotKey key : previous) {
            if (!current.contains(key)) LOGGER.info("Cleared ghost item: {}", key);
        }
    }
}
