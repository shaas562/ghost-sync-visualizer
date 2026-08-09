package com.ghostsync.fabric.client.mixin;

import com.ghostsync.fabric.client.render.GhostBlockRenderer;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
abstract class GameRendererMixin {
    @Inject(method = "close", at = @At("RETURN"))
    private void ghostsync$close(CallbackInfo ci) {
        GhostBlockRenderer.close();
    }
}
