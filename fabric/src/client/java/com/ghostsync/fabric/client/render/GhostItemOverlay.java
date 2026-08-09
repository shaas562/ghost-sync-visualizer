package com.ghostsync.fabric.client.render;

import com.ghostsync.core.SlotKey;
import com.ghostsync.fabric.client.GhostSlotKeys;
import com.ghostsync.fabric.client.GhostSyncRuntime;
import com.ghostsync.fabric.client.config.GhostSyncConfig;
import com.ghostsync.fabric.client.config.GhostSyncConfigManager;
import java.util.Set;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;

/** Read-only visual strength helpers for item slots/cursor stacks already confirmed as ghosts. */
public final class GhostItemOverlay {
    private GhostItemOverlay() {}

    /** Alpha of the original rendered slot model/icon, independent of white overlay. */
    public static float modelAlpha(AbstractContainerMenu menu, Slot slot) {
        GhostSyncConfig config = GhostSyncConfigManager.current();
        if (!config.shouldDetectItems() || !isConfirmedGhost(menu, slot)) return 1.0f;
        return (float) (1.0 - config.items.transparencyStrength);
    }

    /** Strength of the icon-shaped white overlay, independent of original model alpha. */
    public static float overlayAlpha(AbstractContainerMenu menu, Slot slot) {
        GhostSyncConfig config = GhostSyncConfigManager.current();
        if (!config.shouldDetectItems() || !isConfirmedGhost(menu, slot)) return 0.0f;
        return (float) config.items.overlayStrength;
    }

    public static float cursorModelAlpha(AbstractContainerMenu menu) {
        GhostSyncConfig config = GhostSyncConfigManager.current();
        if (!config.shouldDetectItems() || !isConfirmedCursor(menu)) return 1.0f;
        return (float) (1.0 - config.items.transparencyStrength);
    }

    public static float cursorOverlayAlpha(AbstractContainerMenu menu) {
        GhostSyncConfig config = GhostSyncConfigManager.current();
        if (!config.shouldDetectItems() || !isConfirmedCursor(menu)) return 0.0f;
        return (float) config.items.overlayStrength;
    }

    public static boolean isConfirmedGhost(AbstractContainerMenu menu, Slot slot) {
        if (slot.index < 0 || slot.index >= menu.slots.size()) return false;
        Set<SlotKey> confirmed = GhostSyncRuntime.DETECTION.confirmedGhostSlots();
        return !confirmed.isEmpty() && confirmed.contains(GhostSlotKeys.forMenuSlot(menu, slot.index));
    }

    public static boolean isConfirmedCursor(AbstractContainerMenu menu) {
        Set<SlotKey> confirmed = GhostSyncRuntime.DETECTION.confirmedGhostSlots();
        return !confirmed.isEmpty() && confirmed.contains(GhostSlotKeys.forCursor(menu));
    }
}
