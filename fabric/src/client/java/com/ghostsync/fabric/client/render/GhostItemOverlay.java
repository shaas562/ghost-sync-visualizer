package com.ghostsync.fabric.client.render;

import com.ghostsync.core.SlotKey;
import com.ghostsync.fabric.client.GhostSlotKeys;
import com.ghostsync.fabric.client.GhostSyncRuntime;
import com.ghostsync.fabric.client.config.GhostSyncConfig;
import com.ghostsync.fabric.client.config.GhostSyncConfigManager;
import java.util.Set;
import net.minecraft.client.gui.GuiGraphicsExtractor;
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
        if (!config.shouldDetectItems() || !isConfirmedGhost(menu, slot)) return 1.0f;
        return (float) (1.0 - config.items.transparencyStrength);
    }

    /** Single normalized identity predicate shared by overlay and transparency paths. */
    public static boolean isConfirmedGhost(AbstractContainerMenu menu, Slot slot) {
        if (slot.index < 0 || slot.index >= menu.slots.size()) return false;
        Set<SlotKey> confirmed = GhostSyncRuntime.DETECTION.confirmedGhostSlots();
        return !confirmed.isEmpty() && confirmed.contains(GhostSlotKeys.forMenuSlot(menu, slot.index));
    }

    private static int whiteWithAlpha(double strength) {
        int alpha = (int) Math.round(Math.max(0.0, Math.min(1.0, strength)) * 255.0);
        return (alpha << 24) | WHITE_RGB;
    }
}
