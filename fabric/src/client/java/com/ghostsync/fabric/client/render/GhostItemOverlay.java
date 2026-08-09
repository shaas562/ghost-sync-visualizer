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

/** Draws read-only red overlays for item slots already confirmed as ghosts. */
public final class GhostItemOverlay {
    private static final int RED_RGB = 0x00FF2020;

    private GhostItemOverlay() {}

    public static void extractSlot(
            GuiGraphicsExtractor graphics,
            AbstractContainerMenu menu,
            Slot slot) {
        GhostSyncConfig config = GhostSyncConfigManager.current();
        if (!config.shouldDetectItems()) return;

        Set<SlotKey> confirmed = GhostSyncRuntime.DETECTION.confirmedGhostSlots();
        if (confirmed.isEmpty()) return;

        Minecraft client = Minecraft.getInstance();
        long connectionEpoch = GhostSyncRuntime.connectionEpoch();
        long menuEpoch = GhostSyncRuntime.containerEpoch(menu);
        boolean ghost = slot.index >= 0 && confirmed.contains(new SlotKey(
                connectionEpoch,
                menuEpoch,
                menu.containerId,
                slot.index));

        if (!ghost && client.player != null) {
            Inventory inventory = client.player.getInventory();
            int inventorySlot = slot.getContainerSlot();
            ghost = slot.container == inventory
                    && inventorySlot >= 0
                    && inventorySlot < inventory.getContainerSize()
                    && confirmed.contains(GhostSyncRuntime.playerInventoryKey(inventorySlot));
        }

        if (!ghost) return;

        int x = slot.x;
        int y = slot.y;
        if (config.items.overlayStrength > 0.0) {
            graphics.fill(x, y, x + 16, y + 16, redWithAlpha(config.items.overlayStrength));
        }
        if (config.items.transparencyStrength > 0.0) {
            graphics.outline(x, y, 16, 16, redWithAlpha(config.items.transparencyStrength));
        }
    }

    private static int redWithAlpha(double strength) {
        int alpha = (int) Math.round(Math.max(0.0, Math.min(1.0, strength)) * 255.0);
        return (alpha << 24) | RED_RGB;
    }
}
