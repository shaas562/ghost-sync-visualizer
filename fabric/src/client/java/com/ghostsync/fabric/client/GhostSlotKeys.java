package com.ghostsync.fabric.client;

import com.ghostsync.core.SlotKey;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;

/** Normalizes every visible menu item surface onto exactly one detection identity. */
public final class GhostSlotKeys {
    /** Reserved non-negative index outside every real menu slot range. */
    public static final int CURSOR_SLOT_INDEX = Integer.MAX_VALUE;

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

    /**
     * The carried/cursor stack belongs to the currently open menu lifecycle and
     * is invalidated by the same container close/epoch boundary as its menu.
     */
    public static SlotKey forCursor(AbstractContainerMenu menu) {
        return new SlotKey(
                GhostSyncRuntime.connectionEpoch(),
                GhostSyncRuntime.containerEpoch(menu),
                menu.containerId,
                CURSOR_SLOT_INDEX);
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
