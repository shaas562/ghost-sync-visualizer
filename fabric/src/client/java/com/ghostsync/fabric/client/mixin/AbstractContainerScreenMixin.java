package com.ghostsync.fabric.client.mixin;

import com.ghostsync.fabric.client.render.GhostItemOverlay;
import com.ghostsync.fabric.client.render.GhostItemRenderContext;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Carries confirmed slot/cursor visual state through deferred GUI extraction. */
@Mixin(AbstractContainerScreen.class)
abstract class AbstractContainerScreenMixin {
    @Shadow
    public abstract AbstractContainerMenu getMenu();

    @Inject(method = "extractSlot", at = @At("HEAD"))
    private void ghostsync$beforeExtractSlot(
            GuiGraphicsExtractor graphics,
            Slot slot,
            int mouseX,
            int mouseY,
            CallbackInfo ci) {
        GhostItemRenderContext.begin(getMenu(), slot);
    }

    @Inject(method = "extractSlot", at = @At("TAIL"))
    private void ghostsync$afterExtractSlot(
            GuiGraphicsExtractor graphics,
            Slot slot,
            int mouseX,
            int mouseY,
            CallbackInfo ci) {
        try {
            GhostItemOverlay.extractSlot(graphics, getMenu(), slot);
        } finally {
            GhostItemRenderContext.end();
        }
    }

    @Inject(method = "extractCarriedItem", at = @At("HEAD"))
    private void ghostsync$beforeExtractCarriedItem(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            CallbackInfo ci) {
        GhostItemRenderContext.beginCursor(getMenu());
    }

    @Inject(method = "extractCarriedItem", at = @At("TAIL"))
    private void ghostsync$afterExtractCarriedItem(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            CallbackInfo ci) {
        GhostItemRenderContext.end();
    }

    @Inject(method = "extractFloatingItem", at = @At("TAIL"))
    private void ghostsync$afterExtractFloatingItem(
            GuiGraphicsExtractor graphics,
            ItemStack stack,
            int x,
            int y,
            String label,
            CallbackInfo ci) {
        GhostItemOverlay.extractCursor(graphics, x, y);
    }
}
