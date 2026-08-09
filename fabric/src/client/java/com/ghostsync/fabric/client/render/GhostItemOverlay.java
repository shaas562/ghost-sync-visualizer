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

/**
 * Temporary read-only white slot overlay for confirmed ghost items.
 *
 * <p>True item transparency and model-shaped whitening are implemented in the
 * item render-state path; the transparency slider is intentionally not faked
 * as an outline or another unrelated effect here.</p>
 */
public final class GhostItemOverlay {
    private static final int WHITE_RGB = 0x00FFFFFF;

    private GhostItemOverlay() {}

    public static void extractSlot(
            GuiGraphicsExtractor graphics,
            AbstractContainerMenu menu,
            Slot slot) {
        GhostSyncConfig config = GhostSyncConfigManager.current();
        if (!config.shouldDetectItems() || config.items.overlayStrength <= 0.0) return;

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
        graphics.fill(x, y, x + 16, y + 16, whiteWithAlpha(config.items.overlayStrength));
    }

    private static int whiteWithAlpha(double strength) {
        int alpha = (int) Math.round(Math.max(0.0, Math.min(1.0, strength)) * 255.0);
        return (alpha << 24) | WHITE_RGB;
    }
}
