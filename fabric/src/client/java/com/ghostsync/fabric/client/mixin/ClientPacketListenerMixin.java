package com.ghostsync.fabric.client.mixin;

import com.ghostsync.fabric.client.network.Minecraft26PacketAdapter;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Observes vanilla server synchronization after Minecraft applies each packet. */
@Mixin(ClientPacketListener.class)
abstract class ClientPacketListenerMixin {
    @Inject(method = "handleBlockUpdate", at = @At("TAIL"))
    private void ghostsync$afterBlockUpdate(ClientboundBlockUpdatePacket packet, CallbackInfo ci) {
        Minecraft26PacketAdapter.afterBlockUpdate((ClientPacketListener) (Object) this, packet);
    }

    @Inject(method = "handleChunkBlocksUpdate", at = @At("TAIL"))
    private void ghostsync$afterSectionBlocksUpdate(
            ClientboundSectionBlocksUpdatePacket packet,
            CallbackInfo ci) {
        Minecraft26PacketAdapter.afterSectionBlocksUpdate((ClientPacketListener) (Object) this, packet);
    }

    @Inject(method = "handleContainerSetSlot", at = @At("TAIL"))
    private void ghostsync$afterContainerSlot(ClientboundContainerSetSlotPacket packet, CallbackInfo ci) {
        Minecraft26PacketAdapter.afterContainerSlot(packet);
    }

    @Inject(method = "handleContainerContent", at = @At("TAIL"))
    private void ghostsync$afterContainerContent(
            ClientboundContainerSetContentPacket packet,
            CallbackInfo ci) {
        Minecraft26PacketAdapter.afterContainerContent(packet);
    }
}
