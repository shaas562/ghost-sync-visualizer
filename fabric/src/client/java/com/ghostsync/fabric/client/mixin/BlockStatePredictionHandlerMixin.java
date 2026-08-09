package com.ghostsync.fabric.client.mixin;

import com.ghostsync.fabric.client.GhostSyncRuntime;
import net.minecraft.client.multiplayer.prediction.BlockStatePredictionHandler;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Tracks positions vanilla intentionally keeps under client-side prediction. */
@Mixin(BlockStatePredictionHandler.class)
abstract class BlockStatePredictionHandlerMixin {
    @Inject(method = "retainKnownServerState", at = @At("TAIL"))
    private void ghostsync$retainPrediction(BlockPos pos, BlockState state, LocalPlayer player, CallbackInfo ci) {
        int sequence = ((BlockStatePredictionHandler) (Object) this).currentSequence();
        GhostSyncRuntime.retainPredictedBlock(pos, sequence);
    }
}
