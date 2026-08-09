package com.ghostsync.fabric.client.render;

import com.ghostsync.core.BlockKey;
import com.ghostsync.fabric.client.GhostSyncRuntime;
import com.ghostsync.fabric.client.config.GhostSyncConfig;
import com.ghostsync.fabric.client.config.GhostSyncConfigManager;
import java.util.HashSet;
import java.util.Set;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

/**
 * Main-thread snapshot consumed by asynchronous terrain compilation.
 *
 * <p>The section compiler must not scan the synchronized detection map for every
 * ordinary block. This class reduces the hot-path query to an immutable packed
 * position set plus one alpha value. Minecraft 26.2 no longer exposes the old
 * public per-section dirty API, so the first correct implementation coalesces
 * all transparency changes in a tick into one public geometry invalidation.
 * A narrower section rebuild path can replace this fallback after functional
 * Minecraft testing without changing detection or tessellation semantics.</p>
 */
public final class GhostTerrainState {
    private static volatile Snapshot snapshot = Snapshot.EMPTY;
    private static final Set<Long> DIRTY_POSITIONS = new HashSet<>();
    private static boolean initialized;

    private GhostTerrainState() {}

    public static synchronized void initialize() {
        if (initialized) return;
        initialized = true;
        ClientTickEvents.END_CLIENT_TICK.register(GhostTerrainState::refresh);
    }

    public static float modelAlpha(BlockPos pos) {
        Snapshot current = snapshot;
        return current.positions().contains(pos.asLong()) ? current.modelAlpha() : 1.0f;
    }

    public static boolean isTransparentGhost(BlockPos pos) {
        return snapshot.positions().contains(pos.asLong());
    }

    public static synchronized void discardForWorldChange() {
        snapshot = Snapshot.EMPTY;
        DIRTY_POSITIONS.clear();
    }

    private static synchronized void refresh(Minecraft client) {
        Snapshot previous = snapshot;
        Snapshot next = buildSnapshot(client);

        if (!previous.equals(next)) {
            if (Float.compare(previous.modelAlpha(), next.modelAlpha()) != 0) {
                DIRTY_POSITIONS.addAll(previous.positions());
                DIRTY_POSITIONS.addAll(next.positions());
            } else {
                for (long packed : previous.positions()) {
                    if (!next.positions().contains(packed)) DIRTY_POSITIONS.add(packed);
                }
                for (long packed : next.positions()) {
                    if (!previous.positions().contains(packed)) DIRTY_POSITIONS.add(packed);
                }
            }
            snapshot = next;
        }

        invalidateChangedGeometry(client);
    }

    private static void invalidateChangedGeometry(Minecraft client) {
        if (DIRTY_POSITIONS.isEmpty() || client.level == null) return;

        // Coalesce any number of ghost/config changes observed during this tick
        // into a single renderer invalidation instead of rebuilding once per block.
        DIRTY_POSITIONS.clear();
        client.levelRenderer.invalidateCompiledGeometry(
                client.level,
                client.options,
                client.gameRenderer.getMainCamera(),
                client.getBlockColors());
    }

    private static Snapshot buildSnapshot(Minecraft client) {
        GhostSyncConfig config = GhostSyncConfigManager.current();
        if (client.level == null
                || client.player == null
                || !config.shouldDetectBlocks()
                || config.blocks.transparencyStrength <= 0.0) {
            return Snapshot.EMPTY;
        }

        long connectionEpoch = GhostSyncRuntime.connectionEpoch();
        long worldEpoch = GhostSyncRuntime.worldEpoch();
        String dimensionId = client.level.dimension().identifier().toString();
        int playerX = client.player.blockPosition().getX();
        int playerZ = client.player.blockPosition().getZ();
        long maxDistance = (long) config.blocks.detectionDistanceChunks * 16L;
        long maxDistanceSquared = maxDistance * maxDistance;

        Set<Long> positions = new HashSet<>();
        for (BlockKey key : GhostSyncRuntime.DETECTION.confirmedGhostBlocks()) {
            if (key.connectionEpoch() != connectionEpoch
                    || key.worldEpoch() != worldEpoch
                    || !key.dimensionId().equals(dimensionId)) continue;

            long dx = (long) key.x() - playerX;
            long dz = (long) key.z() - playerZ;
            if (dx * dx + dz * dz > maxDistanceSquared) continue;

            BlockPos pos = new BlockPos(key.x(), key.y(), key.z());
            if (client.level.hasChunkAt(pos)) positions.add(pos.asLong());
        }

        if (positions.isEmpty()) return Snapshot.EMPTY;
        return new Snapshot(Set.copyOf(positions), (float) (1.0 - config.blocks.transparencyStrength));
    }

    private record Snapshot(Set<Long> positions, float modelAlpha) {
        private static final Snapshot EMPTY = new Snapshot(Set.of(), 1.0f);
    }
}
