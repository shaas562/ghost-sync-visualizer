package com.ghostsync.fabric.client.render;

import com.ghostsync.core.SlotKey;
import com.ghostsync.fabric.client.GhostSyncRuntime;
import com.ghostsync.fabric.client.config.GhostSyncConfig;
import com.ghostsync.fabric.client.config.GhostSyncConfigManager;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;

/** Read-only visual helpers for item slots already confirmed as ghosts. */
public final class GhostItemOverlay {
    private static final int WHITE_RGB = 0x00FFFFFF;

    private GhostItemOverlay() {}

    public static void extractSlot(
            GuiGraphicsExtractor graphics,
            AbstractContainerMenu menu,
            Slot slot) {
        GhostSyncConfig config = GhostSyncConfigManager.current();
        if (!config.shouldDetectItems()
                || config.items.overlayStrength <= 0.0
                || !isConfirmedGhost(menu, slot)) {
            return;
        }

        // This pass remains independent from original-icon alpha. A later renderer
        // pass can replace the rectangle with an atlas-alpha silhouette without
        // changing certainty or transparency semantics.
        graphics.fill(
                slot.x,
                slot.y,
                slot.x + 16,
                slot.y + 16,
                whiteWithAlpha(config.items.overlayStrength));
    }

    /** Alpha of the original rendered item model/icon, independent of white overlay. */
    public static float modelAlpha(AbstractContainerMenu menu, Slot slot) {
        GhostSyncConfig config = GhostSyncConfigManager.current();
        if (!config.shouldDetectItems() || !isConfirmedGhost(menu, slot)) {
            return 1.0f;
        }
        return (float) (1.0 - config.items.transparencyStrength);
    }

    /** Single identity predicate shared by overlay and transparency paths. */
    public static boolean isConfirmedGhost(AbstractContainerMenu menu, Slot slot) {
        Set<SlotKey> confirmed = GhostSyncRuntime.DETECTION.confirmedGhostSlots();
        if (confirmed.isEmpty()) return false;

        Minecraft client = Minecraft.getInstance();
        long connectionEpoch = GhostSyncRuntime.connectionEpoch();
        long menuEpoch = GhostSyncRuntime.containerEpoch(menu);

        if (slot.index >= 0 && confirmed.contains(new SlotKey(
                connectionEpoch,
                menuEpoch,
                menu.containerId,
                slot.index))) {
            return true;
        }

        if (client.player == null) return false;
        Inventory inventory = client.player.getInventory();
        int inventorySlot = slot.getContainerSlot();
        return slot.container == inventory
                && inventorySlot >= 0
                && inventorySlot < inventory.getContainerSize()
                && confirmed.contains(GhostSyncRuntime.playerInventoryKey(inventorySlot));
    }

    private static int whiteWithAlpha(double strength) {
        int alpha = (int) Math.round(Math.max(0.0, Math.min(1.0, strength)) * 255.0);
        return (alpha << 24) | WHITE_RGB;
    }
}
