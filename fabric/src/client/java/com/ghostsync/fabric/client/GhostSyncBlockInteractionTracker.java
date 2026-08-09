package com.ghostsync.fabric.client;

import com.ghostsync.core.BlockKey;
import com.ghostsync.core.Presence;
import com.ghostsync.fabric.client.config.GhostSyncConfigManager;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;

/**
 * Watches blocks touched by real player interactions for local predicted
 * presence changes. It never cancels or modifies the interaction and can only
 * create PENDING state, never a confirmed ghost.
 */
public final class GhostSyncBlockInteractionTracker {
    private static final int WATCH_TICKS = 8;
    private static final Map<BlockPos, Watch> WATCHED = new HashMap<>();

    private GhostSyncBlockInteractionTracker() {}

    public static void initialize() {
        UseBlockCallback.EVENT.register((player, level, hand, hitResult) -> {
            Minecraft client = Minecraft.getInstance();
            if (!level.isClientSide()
                    || player != client.player
                    || !(level instanceof ClientLevel clientLevel)
                    || !GhostSyncConfigManager.current().shouldDetectBlocks()) {
                return InteractionResult.PASS;
            }
            BlockPos target = hitResult.getBlockPos();
            watch(clientLevel, target);
            watch(clientLevel, target.relative(hitResult.getDirection()));
            return InteractionResult.PASS;
        });
        ClientTickEvents.END_CLIENT_TICK.register(GhostSyncBlockInteractionTracker::onEndTick);
    }

    private static void watch(ClientLevel level, BlockPos pos) {
        if (!level.hasChunkAt(pos)) return;
        WATCHED.put(pos.immutable(), new Watch(presence(level, pos), WATCH_TICKS));
    }

    private static void onEndTick(Minecraft client) {
        ClientLevel level = client.level;
        if (level == null || !GhostSyncConfigManager.current().shouldDetectBlocks()) {
            WATCHED.clear();
            return;
        }
        Iterator<Map.Entry<BlockPos, Watch>> iterator = WATCHED.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<BlockPos, Watch> entry = iterator.next();
            BlockPos pos = entry.getKey();
            Watch watch = entry.getValue();
            if (!level.hasChunkAt(pos)) {
                iterator.remove();
                continue;
            }
            Presence current = presence(level, pos);
            if (current != watch.lastPresence) {
                GhostSyncRuntime.DETECTION.blocks().receiveClientMutation(blockKey(level, pos), current);
                watch.lastPresence = current;
            }
            if (--watch.remainingTicks <= 0) iterator.remove();
        }
    }

    private static BlockKey blockKey(ClientLevel level, BlockPos pos) {
        return new BlockKey(
                GhostSyncRuntime.connectionEpoch(),
                GhostSyncRuntime.worldEpoch(),
                level.dimension().identifier().toString(),
                pos.getX(), pos.getY(), pos.getZ());
    }

    private static Presence presence(ClientLevel level, BlockPos pos) {
        return level.getBlockState(pos).isAir() ? Presence.ABSENT : Presence.PRESENT;
    }

    private static final class Watch {
        private Presence lastPresence;
        private int remainingTicks;

        private Watch(Presence lastPresence, int remainingTicks) {
            this.lastPresence = lastPresence;
            this.remainingTicks = remainingTicks;
        }
    }
}
