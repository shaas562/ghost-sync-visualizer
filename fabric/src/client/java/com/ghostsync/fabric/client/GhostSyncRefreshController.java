package com.ghostsync.fabric.client;

import com.ghostsync.core.BlockKey;
import com.ghostsync.core.Presence;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;

/** Executes manual refresh requests against trustworthy state already known locally. */
public final class GhostSyncRefreshController {
    private static long handledGeneration;

    private GhostSyncRefreshController() {}

    public static void initialize() {
        handledGeneration = GhostSyncRuntime.refreshGeneration();
        ClientTickEvents.END_CLIENT_TICK.register(GhostSyncRefreshController::onEndTick);
    }

    private static void onEndTick(Minecraft client) {
        long requested = GhostSyncRuntime.refreshGeneration();
        if (requested == handledGeneration) return;
        handledGeneration = requested;

        ClientLevel level = client.level;
        if (level == null) return;

        long connectionEpoch = GhostSyncRuntime.connectionEpoch();
        long worldEpoch = GhostSyncRuntime.worldEpoch();
        String dimensionId = level.dimension().identifier().toString();

        for (BlockKey key : GhostSyncRuntime.DETECTION.blocks().trackedKeys()) {
            if (key.connectionEpoch() != connectionEpoch
                    || key.worldEpoch() != worldEpoch
                    || !key.dimensionId().equals(dimensionId)) continue;

            BlockPos pos = new BlockPos(key.x(), key.y(), key.z());
            if (!level.hasChunkAt(pos)) continue;

            Presence presence = level.getBlockState(pos).isAir()
                    ? Presence.ABSENT
                    : Presence.PRESENT;
            GhostSyncRuntime.DETECTION.blocks().observeClientState(key, presence);
        }

        // Item state is already re-observed every client tick by
        // GhostSyncLifecycleTracker. This refresh intentionally does not fabricate
        // inventory actions or server requests merely to provoke a correction.
    }
}
