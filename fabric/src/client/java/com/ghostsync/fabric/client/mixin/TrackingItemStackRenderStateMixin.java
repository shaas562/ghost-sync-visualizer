package com.ghostsync.fabric.client.mixin;

import com.ghostsync.fabric.client.render.GhostItemRenderContext;
import com.ghostsync.fabric.client.render.GhostItemRenderStateAccess;
import net.minecraft.client.renderer.item.TrackingItemStackRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Persists ghost visual strengths from extraction until the deferred GUI item blit. */
@Mixin(TrackingItemStackRenderState.class)
abstract class TrackingItemStackRenderStateMixin implements GhostItemRenderStateAccess {
    @Unique
    private float ghostsync$alpha = 1.0f;
    @Unique
    private float ghostsync$overlayAlpha;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void ghostsync$captureVisualStrengths(CallbackInfo ci) {
        ghostsync$alpha = GhostItemRenderContext.currentAlpha();
        ghostsync$overlayAlpha = GhostItemRenderContext.currentOverlayAlpha();
    }

    @Override
    public float ghostsync$getAlpha() {
        return ghostsync$alpha;
    }

    @Override
    public void ghostsync$setAlpha(float alpha) {
        ghostsync$alpha = Math.max(0.0f, Math.min(1.0f, alpha));
    }

    @Override
    public float ghostsync$getOverlayAlpha() {
        return ghostsync$overlayAlpha;
    }

    @Override
    public void ghostsync$setOverlayAlpha(float alpha) {
        ghostsync$overlayAlpha = Math.max(0.0f, Math.min(1.0f, alpha));
    }
}
