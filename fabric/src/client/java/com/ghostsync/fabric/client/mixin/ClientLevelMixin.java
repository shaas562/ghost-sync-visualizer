package com.ghostsync.fabric.client.mixin;

import com.ghostsync.fabric.client.network.Minecraft26PacketAdapter;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Captures server-verified block prediction resolution after vanilla applies it. */
@Mixin(ClientLevel.class)
abstract class ClientLevelMixin {
    @Inject(method = "syncBlockState", at = @At("TAIL"))
    private void ghostsync$afterSyncBlockState(
            BlockPos pos,
            BlockState knownServerState,
            Vec3 playerPosition,
            CallbackInfo ci) {
        Minecraft26PacketAdapter.afterPredictedBlockSync(
                (ClientLevel) (Object) this,
                pos,
                knownServerState);
    }
}
