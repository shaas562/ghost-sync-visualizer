package com.ghostsync.fabric.client.network;

import com.ghostsync.core.BlockKey;
import com.ghostsync.core.Presence;
import com.ghostsync.core.SlotKey;
import com.ghostsync.fabric.client.GhostSyncRuntime;
import com.ghostsync.fabric.client.config.GhostSyncConfigManager;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Minecraft 26.2 packet-to-core translation. No Minecraft packet class crosses
 * this adapter boundary into the version-independent core module.
 */
public final class Minecraft26PacketAdapter {
    private Minecraft26PacketAdapter() {
    }

    public static void afterBlockUpdate(
            ClientPacketListener listener,
            ClientboundBlockUpdatePacket packet) {
        if (!GhostSyncConfigManager.current().shouldDetectBlocks()) {
            return;
        }
        ClientLevel level = listener.getLevel();
        if (level == null) {
            return;
        }

        long sequence = GhostSyncRuntime.nextServerSequence();
        recordBlock(level, packet.getPos(), packet.getBlockState(), sequence);
    }

    public static void afterSectionBlocksUpdate(
            ClientPacketListener listener,
            ClientboundSectionBlocksUpdatePacket packet) {
        if (!GhostSyncConfigManager.current().shouldDetectBlocks()) {
            return;
        }
        ClientLevel level = listener.getLevel();
        if (level == null) {
            return;
        }

        long sequence = GhostSyncRuntime.nextServerSequence();
        packet.runUpdates((pos, serverState) -> recordBlock(level, pos, serverState, sequence));
    }

    public static void afterContainerSlot(ClientboundContainerSetSlotPacket packet) {
        if (!GhostSyncConfigManager.current().shouldDetectItems() || packet.getSlot() < 0) {
            return;
        }

        AbstractContainerMenu menu = findMenu(packet.getContainerId());
        if (menu == null || packet.getSlot() >= menu.slots.size()) {
            return;
        }

        long sequence = GhostSyncRuntime.nextServerSequence();
        recordSlot(menu, packet.getSlot(), packet.getItem(), sequence);
    }

    public static void afterContainerContent(ClientboundContainerSetContentPacket packet) {
        if (!GhostSyncConfigManager.current().shouldDetectItems()) {
            return;
        }

        AbstractContainerMenu menu = findMenu(packet.containerId());
        if (menu == null) {
            return;
        }

        long sequence = GhostSyncRuntime.nextServerSequence();
        List<ItemStack> items = packet.items();
        int count = Math.min(items.size(), menu.slots.size());
        for (int slot = 0; slot < count; slot++) {
            recordSlot(menu, slot, items.get(slot), sequence);
        }
    }

    private static void recordBlock(
            ClientLevel level,
            BlockPos pos,
            BlockState serverState,
            long sequence) {
        BlockKey key = new BlockKey(
                GhostSyncRuntime.connectionEpoch(),
                GhostSyncRuntime.worldEpoch(),
                level.dimension().identifier().toString(),
                pos.getX(),
                pos.getY(),
                pos.getZ());

        Presence serverPresence = serverState.isAir() ? Presence.ABSENT : Presence.PRESENT;
        Presence clientPresence = level.getBlockState(pos).isAir() ? Presence.ABSENT : Presence.PRESENT;

        GhostSyncRuntime.DETECTION.blocks().receiveAuthoritativeState(key, serverPresence, sequence);
        GhostSyncRuntime.DETECTION.blocks().observeClientAfterAuthoritativeState(
                key, clientPresence, sequence);
    }

    private static void recordSlot(
            AbstractContainerMenu menu,
            int slot,
            ItemStack serverStack,
            long sequence) {
        long menuEpoch = GhostSyncRuntime.containerEpoch(menu);
        SlotKey key = new SlotKey(
                GhostSyncRuntime.connectionEpoch(),
                menuEpoch,
                menu.containerId,
                slot);

        Presence serverPresence = serverStack.isEmpty() ? Presence.ABSENT : Presence.PRESENT;
        Presence clientPresence = menu.getSlot(slot).getItem().isEmpty()
                ? Presence.ABSENT
                : Presence.PRESENT;

        GhostSyncRuntime.DETECTION.slots().receiveAuthoritativeState(key, serverPresence, sequence);
        GhostSyncRuntime.DETECTION.slots().observeClientAfterAuthoritativeState(
                key, clientPresence, sequence);
    }

    private static AbstractContainerMenu findMenu(int containerId) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return null;
        }
        if (client.player.containerMenu.containerId == containerId) {
            return client.player.containerMenu;
        }
        if (client.player.inventoryMenu.containerId == containerId) {
            return client.player.inventoryMenu;
        }
        return null;
    }
}
