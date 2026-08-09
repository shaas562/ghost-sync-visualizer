package com.ghostsync.fabric.client.render;

import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;

/**
 * Short-lived extraction context. Minecraft creates the TrackingItemStackRenderState
 * while AbstractContainerScreen extracts a slot, then renders that state later.
 */
public final class GhostItemRenderContext {
    private static final ThreadLocal<Float> ALPHA = ThreadLocal.withInitial(() -> 1.0f);

    private GhostItemRenderContext() {}

    public static void begin(AbstractContainerMenu menu, Slot slot) {
        ALPHA.set(GhostItemOverlay.modelAlpha(menu, slot));
    }

    public static float currentAlpha() {
        return ALPHA.get();
    }

    public static void end() {
        ALPHA.remove();
    }
}
