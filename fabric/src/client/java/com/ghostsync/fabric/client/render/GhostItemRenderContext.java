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
        STATE.set(new State(GhostItemOverlay.modelAlpha(menu, slot), false));
    }

    public static void beginCursor(AbstractContainerMenu menu) {
        boolean ghost = GhostItemOverlay.isConfirmedCursor(menu);
        STATE.set(new State(GhostItemOverlay.cursorModelAlpha(menu), ghost));
    }

    public static float currentAlpha() {
        return STATE.get().alpha();
    }

    public static boolean isCursorGhost() {
        return STATE.get().cursorGhost();
    }

    public static void end() {
        STATE.remove();
    }

    private record State(float alpha, boolean cursorGhost) {
        private static State normal() {
            return new State(1.0f, false);
        }
    }
}
