package com.ghostsync.fabric.client.render;

import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;

/**
 * Short-lived extraction context. Minecraft creates TrackingItemStackRenderState
 * objects while a container screen extracts slots/cursor items, then renders
 * those states later.
 */
public final class GhostItemRenderContext {
    private static final ThreadLocal<State> STATE = ThreadLocal.withInitial(State::normal);

    private GhostItemRenderContext() {}

    public static void begin(AbstractContainerMenu menu, Slot slot) {
        STATE.set(new State(
                GhostItemOverlay.modelAlpha(menu, slot),
                GhostItemOverlay.overlayAlpha(menu, slot)));
    }

    public static void beginCursor(AbstractContainerMenu menu) {
        STATE.set(new State(
                GhostItemOverlay.cursorModelAlpha(menu),
                GhostItemOverlay.cursorOverlayAlpha(menu)));
    }

    public static float currentAlpha() {
        return STATE.get().alpha();
    }

    public static float currentOverlayAlpha() {
        return STATE.get().overlayAlpha();
    }

    public static void end() {
        STATE.remove();
    }

    private record State(float alpha, float overlayAlpha) {
        private static State normal() {
            return new State(1.0f, 0.0f);
        }
    }
}
