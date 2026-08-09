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

/** Read-only visual helpers for item slots/cursor stacks already confirmed as ghosts. */
public final class GhostItemOverlay {
    private static final int WHITE_RGB = 0x00FFFFFF;

    private GhostItemOverlay() {}

    public static void extractSlot(GuiGraphicsExtractor graphics, AbstractContainerMenu menu, Slot slot) {
        GhostSyncConfig config = GhostSyncConfigManager.current();
        if (!config.shouldDetectItems()
                || config.items.overlayStrength <= 0.0
                || !isConfirmedGhost(menu, slot)) {
            return;
        }
        graphics.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, whiteWithAlpha(config.items.overlayStrength));
    }

    public static void extractCursor(GuiGraphicsExtractor graphics, int x, int y) {
        GhostSyncConfig config = GhostSyncConfigManager.current();
        if (!config.shouldDetectItems()
                || config.items.overlayStrength <= 0.0
                || !GhostItemRenderContext.isCursorGhost()) {
            return;
        }
        graphics.fill(x, y, x + 16, y + 16, whiteWithAlpha(config.items.overlayStrength));
    }

    /** Alpha of the original rendered slot model/icon, independent of white overlay. */
    public static float modelAlpha(AbstractContainerMenu menu, Slot slot) {
        GhostSyncConfig config = GhostSyncConfigManager.current();
        if (!config.shouldDetectItems() || !isConfirmedGhost(menu, slot)) return 1.0f;
        return (float) (1.0 - config.items.transparencyStrength);
    }

    /** Alpha of the original carried/cursor icon, using the same independent control. */
    public static float cursorModelAlpha(AbstractContainerMenu menu) {
        GhostSyncConfig config = GhostSyncConfigManager.current();
        if (!config.shouldDetectItems() || !isConfirmedCursor(menu)) return 1.0f;
        return (float) (1.0 - config.items.transparencyStrength);
    }

    /** Single normalized identity predicate shared by slot overlay/transparency paths. */
    public static boolean isConfirmedGhost(AbstractContainerMenu menu, Slot slot) {
        if (slot.index < 0 || slot.index >= menu.slots.size()) return false;
        Set<SlotKey> confirmed = GhostSyncRuntime.DETECTION.confirmedGhostSlots();
        return !confirmed.isEmpty() && confirmed.contains(GhostSlotKeys.forMenuSlot(menu, slot.index));
    }

    public static boolean isConfirmedCursor(AbstractContainerMenu menu) {
        Set<SlotKey> confirmed = GhostSyncRuntime.DETECTION.confirmedGhostSlots();
        return !confirmed.isEmpty() && confirmed.contains(GhostSlotKeys.forCursor(menu));
    }

    private static int whiteWithAlpha(double strength) {
        int alpha = (int) Math.round(Math.max(0.0, Math.min(1.0, strength)) * 255.0);
        return (alpha << 24) | WHITE_RGB;
    }
}
