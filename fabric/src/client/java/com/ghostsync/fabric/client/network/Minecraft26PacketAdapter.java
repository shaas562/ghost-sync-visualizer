package com.ghostsync.fabric.client.network;

import com.ghostsync.core.BlockKey;
import com.ghostsync.core.Presence;
import com.ghostsync.core.SlotKey;
import com.ghostsync.fabric.client.GhostSlotKeys;
import com.ghostsync.fabric.client.GhostSyncRuntime;
import com.ghostsync.fabric.client.config.GhostSyncConfigManager;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundBlockChangedAckPacket;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundForgetLevelChunkPacket;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSetPlayerInventoryPacket;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;

/** Minecraft 26.2 packet-to-core translation. */
public final class Minecraft26PacketAdapter {
    private Minecraft26PacketAdapter() {}

    public static void afterBlockUpdate(ClientPacketListener listener, ClientboundBlockUpdatePacket packet) {
        if (!GhostSyncConfigManager.current().shouldDetectBlocks()) return;
        ClientLevel level = listener.getLevel();
        if (level == null) return;
        long sequence = GhostSyncRuntime.nextServerSequence();
        recordBlockEvidence(level, packet.getPos(), packet.getBlockState(), sequence,
                !GhostSyncRuntime.isBlockPredictionPending(packet.getPos()));
    }

    public static void afterSectionBlocksUpdate(ClientPacketListener listener, ClientboundSectionBlocksUpdatePacket packet) {
        if (!GhostSyncConfigManager.current().shouldDetectBlocks()) return;
        ClientLevel level = listener.getLevel();
        if (level == null) return;
        long sequence = GhostSyncRuntime.nextServerSequence();
        packet.runUpdates((pos, serverState) -> recordBlockEvidence(
                level, pos, serverState, sequence, !GhostSyncRuntime.isBlockPredictionPending(pos)));
    }

    public static void afterForgetLevelChunk(ClientPacketListener listener, ClientboundForgetLevelChunkPacket packet) {
        ClientLevel level = listener.getLevel();
        if (level == null) return;
        ChunkPos pos = packet.pos();
        GhostSyncRuntime.DETECTION.unloadChunk(
                GhostSyncRuntime.connectionEpoch(), GhostSyncRuntime.worldEpoch(),
                level.dimension().identifier().toString(), pos.x(), pos.z());
    }

    /** ACK retires local prediction bookkeeping; it is not authority by itself. */
    public static void afterBlockChangedAck(ClientboundBlockChangedAckPacket packet) {
        GhostSyncRuntime.acknowledgeBlockPredictions(packet.sequence());
    }

    /** Records the exact server-verified state vanilla is about to sync after a prediction ACK. */
    public static long beforePredictionSync(ClientLevel level, BlockPos pos, BlockState serverVerifiedState) {
        if (!GhostSyncConfigManager.current().shouldDetectBlocks()) return -1L;
        long sequence = GhostSyncRuntime.nextServerSequence();
        GhostSyncRuntime.DETECTION.blocks().receiveAuthoritativeState(
                blockKey(level, pos), presence(serverVerifiedState), sequence);
        return sequence;
    }

    /** Pairs post-sync client state with the exact server-verified state recorded at HEAD. */
    public static void afterPredictionSync(ClientLevel level, BlockPos pos, long sequence) {
        if (sequence < 0) return;
        GhostSyncRuntime.DETECTION.blocks().observeClientAfterAuthoritativeState(
                blockKey(level, pos), presence(level.getBlockState(pos)), sequence);
    }

    public static void afterContainerSlot(ClientboundContainerSetSlotPacket packet) {
        if (!GhostSyncConfigManager.current().shouldDetectItems() || packet.getSlot() < 0) return;
        AbstractContainerMenu menu = findMenu(packet.getContainerId());
        if (menu == null || packet.getSlot() >= menu.slots.size() || menu.getStateId() != packet.getStateId()) return;
        long sequence = GhostSyncRuntime.nextServerSequence();
        recordSlot(menu, packet.getSlot(), packet.getItem(), sequence);
    }

    public static void afterContainerContent(ClientboundContainerSetContentPacket packet) {
        if (!GhostSyncConfigManager.current().shouldDetectItems()) return;
        AbstractContainerMenu menu = findMenu(packet.containerId());
        if (menu == null || menu.getStateId() != packet.stateId()) return;
        long sequence = GhostSyncRuntime.nextServerSequence();
        List<ItemStack> items = packet.items();
        int count = Math.min(items.size(), menu.slots.size());
        for (int slot = 0; slot < count; slot++) recordSlot(menu, slot, items.get(slot), sequence);
    }

    public static void afterPlayerInventory(ClientboundSetPlayerInventoryPacket packet) {
        if (!GhostSyncConfigManager.current().shouldDetectItems() || packet.slot() < 0) return;
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;
        Inventory inventory = client.player.getInventory();
        if (packet.slot() >= inventory.getContainerSize()) return;
        SlotKey key = GhostSyncRuntime.playerInventoryKey(packet.slot());
        long sequence = GhostSyncRuntime.nextServerSequence();
        GhostSyncRuntime.DETECTION.slots().receiveAuthoritativeState(key, presence(packet.contents()), sequence);
        GhostSyncRuntime.DETECTION.slots().observeClientAfterAuthoritativeState(
                key, presence(inventory.getItem(packet.slot())), sequence);
    }

    private static void recordBlockEvidence(ClientLevel level, BlockPos pos, BlockState serverState,
            long sequence, boolean pairClientState) {
        BlockKey key = blockKey(level, pos);
        GhostSyncRuntime.DETECTION.blocks().receiveAuthoritativeState(key, presence(serverState), sequence);
        if (pairClientState) {
            GhostSyncRuntime.DETECTION.blocks().observeClientAfterAuthoritativeState(
                    key, presence(level.getBlockState(pos)), sequence);
        }
    }

    private static void recordSlot(AbstractContainerMenu menu, int slot, ItemStack serverStack, long sequence) {
        SlotKey key = GhostSlotKeys.forMenuSlot(menu, slot);
        GhostSyncRuntime.DETECTION.slots().receiveAuthoritativeState(key, presence(serverStack), sequence);
        GhostSyncRuntime.DETECTION.slots().observeClientAfterAuthoritativeState(
                key, presence(menu.getSlot(slot).getItem()), sequence);
    }

    private static BlockKey blockKey(ClientLevel level, BlockPos pos) {
        return new BlockKey(GhostSyncRuntime.connectionEpoch(), GhostSyncRuntime.worldEpoch(),
                level.dimension().identifier().toString(), pos.getX(), pos.getY(), pos.getZ());
    }

    private static Presence presence(BlockState state) { return state.isAir() ? Presence.ABSENT : Presence.PRESENT; }
    private static Presence presence(ItemStack stack) { return stack.isEmpty() ? Presence.ABSENT : Presence.PRESENT; }

    private static AbstractContainerMenu findMenu(int containerId) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return null;
        if (client.player.containerMenu.containerId == containerId) return client.player.containerMenu;
        if (client.player.inventoryMenu.containerId == containerId) return client.player.inventoryMenu;
        return null;
    }
}
