package com.ghostsync.fabric.client;

import com.ghostsync.core.SlotKey;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;

/** Normalizes every visible menu slot onto exactly one detection identity. */
public final class GhostSlotKeys {
    private GhostSlotKeys() {}

    public static SlotKey forMenuSlot(AbstractContainerMenu menu, int slotIndex) {
        Slot slot = menu.getSlot(slotIndex);
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            Inventory inventory = client.player.getInventory();
            int inventorySlot = slot.getContainerSlot();
            if (slot.container == inventory
                    && inventorySlot >= 0
                    && inventorySlot < inventory.getContainerSize()) {
                return GhostSyncRuntime.playerInventoryKey(inventorySlot);
            }
        }

        return new SlotKey(
                GhostSyncRuntime.connectionEpoch(),
                GhostSyncRuntime.containerEpoch(menu),
                menu.containerId,
                slotIndex);
    }

    public static boolean isPlayerInventoryBacked(AbstractContainerMenu menu, int slotIndex) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return false;
        Slot slot = menu.getSlot(slotIndex);
        Inventory inventory = client.player.getInventory();
        int inventorySlot = slot.getContainerSlot();
        return slot.container == inventory
                && inventorySlot >= 0
                && inventorySlot < inventory.getContainerSize();
    }
}
