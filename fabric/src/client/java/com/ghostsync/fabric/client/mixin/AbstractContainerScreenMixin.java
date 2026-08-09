package com.ghostsync.fabric.client.mixin;

import com.ghostsync.fabric.client.render.GhostItemOverlay;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Adds confirmed-ghost overlays immediately after vanilla extracts each slot. */
@Mixin(AbstractContainerScreen.class)
abstract class AbstractContainerScreenMixin {
    @Shadow
    public abstract AbstractContainerMenu getMenu();

    @Inject(method = "extractSlot", at = @At("TAIL"))
    private void ghostsync$afterExtractSlot(
            GuiGraphicsExtractor graphics,
            Slot slot,
            int mouseX,
            int mouseY,
            CallbackInfo ci) {
        GhostItemOverlay.extractSlot(graphics, getMenu(), slot);
    }
}
