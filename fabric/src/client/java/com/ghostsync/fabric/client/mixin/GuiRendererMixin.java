package com.ghostsync.fabric.client.mixin;

import com.ghostsync.fabric.client.render.GhostItemRenderStateAccess;
import com.ghostsync.fabric.client.render.GhostItemSilhouettePipeline;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import net.minecraft.client.gui.render.GuiItemAtlas;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.state.gui.BlitRenderState;
import net.minecraft.client.renderer.state.gui.GuiItemRenderState;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Applies independent original-icon transparency and icon-shaped white overlay. */
@Mixin(GuiRenderer.class)
abstract class GuiRendererMixin {
    @Shadow
    @Final
    private GuiRenderState renderState;

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

    @Inject(method = "submitBlitFromItemAtlas", at = @At("TAIL"))
    private void ghostsync$addWhiteSilhouette(
            GuiItemRenderState itemState,
            GuiItemAtlas.SlotView slotView,
            CallbackInfo ci) {
        float overlayAlpha = ((GhostItemRenderStateAccess) itemState.itemStackRenderState())
                .ghostsync$getOverlayAlpha();
        if (overlayAlpha <= 0.0001f) return;

        int alphaByte = Math.clamp(Math.round(overlayAlpha * 255.0f), 0, 255);
        int white = (alphaByte << 24) | 0x00FFFFFF;
        TextureSetup texture = TextureSetup.singleTexture(
                slotView.textureView(),
                RenderSystem.getSamplerCache().getRepeat(FilterMode.NEAREST));

        renderState.addBlitToCurrentLayer(new BlitRenderState(
                GhostItemSilhouettePipeline.PIPELINE,
                texture,
                itemState.pose(),
                itemState.x(),
                itemState.y(),
                itemState.x() + 16,
                itemState.y() + 16,
                slotView.u0(),
                slotView.u1(),
                slotView.v0(),
                slotView.v1(),
                white,
                itemState.scissorArea(),
                null));
    }
}
