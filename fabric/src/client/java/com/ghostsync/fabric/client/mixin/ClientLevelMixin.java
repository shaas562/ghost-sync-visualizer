package com.ghostsync.fabric.client.mixin;

import com.ghostsync.fabric.client.network.Minecraft26PacketAdapter;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Pairs vanilla's retained server-verified prediction state with the final client state. */
@Mixin(ClientLevel.class)
abstract class ClientLevelMixin {
    @Unique private long ghostsync$predictionSyncSequence = -1L;

    @Inject(method = "syncBlockState", at = @At("HEAD"))
    private void ghostsync$beforeSyncBlockState(BlockPos pos, BlockState serverState, Vec3 playerPos, CallbackInfo ci) {
        ghostsync$predictionSyncSequence = Minecraft26PacketAdapter.beforePredictionSync(
                (ClientLevel) (Object) this, pos, serverState);
    }

    @Inject(method = "syncBlockState", at = @At("TAIL"))
    private void ghostsync$afterSyncBlockState(BlockPos pos, BlockState serverState, Vec3 playerPos, CallbackInfo ci) {
        long sequence = ghostsync$predictionSyncSequence;
        ghostsync$predictionSyncSequence = -1L;
        Minecraft26PacketAdapter.afterPredictionSync((ClientLevel) (Object) this, pos, sequence);
    }
}
