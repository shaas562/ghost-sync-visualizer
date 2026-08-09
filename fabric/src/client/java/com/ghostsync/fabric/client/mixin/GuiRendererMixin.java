package com.ghostsync.fabric.client.mixin;

import com.ghostsync.fabric.client.render.GhostItemRenderStateAccess;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.gui.render.GuiItemAtlas;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.renderer.state.gui.GuiItemRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Applies original-icon transparency at the final atlas blit. Because normal
 * and special item models are already rendered into GuiItemAtlas at this point,
 * one alpha multiplier covers both paths without mutating shared model data.
 */
@Mixin(GuiRenderer.class)
abstract class GuiRendererMixin {
    @ModifyExpressionValue(
            method = "submitBlitFromItemAtlas",
            at = @At(value = "CONSTANT", args = "intValue=-1"))
    private int ghostsync$applyConfirmedGhostAlpha(
            int originalColor,
            GuiItemRenderState itemState,
            GuiItemAtlas.SlotView slotView) {
        float alpha = ((GhostItemRenderStateAccess) itemState.itemStackRenderState()).ghostsync$getAlpha();
        if (alpha >= 0.9999f) return originalColor;
        int alphaByte = Math.clamp(Math.round(alpha * 255.0f), 0, 255);
        return (alphaByte << 24) | 0x00FFFFFF;
    }
}
