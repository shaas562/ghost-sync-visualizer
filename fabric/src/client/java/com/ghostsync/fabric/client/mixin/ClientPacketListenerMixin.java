package com.ghostsync.fabric.client.mixin;

import com.ghostsync.fabric.client.network.Minecraft26PacketAdapter;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockChangedAckPacket;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundForgetLevelChunkPacket;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSetPlayerInventoryPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
abstract class ClientPacketListenerMixin {
    @Inject(method = "handleBlockUpdate", at = @At("TAIL"))
    private void ghostsync$afterBlockUpdate(ClientboundBlockUpdatePacket packet, CallbackInfo ci) {
        Minecraft26PacketAdapter.afterBlockUpdate((ClientPacketListener) (Object) this, packet);
    }

    @Inject(method = "handleChunkBlocksUpdate", at = @At("TAIL"))
    private void ghostsync$afterSectionBlocksUpdate(ClientboundSectionBlocksUpdatePacket packet, CallbackInfo ci) {
        Minecraft26PacketAdapter.afterSectionBlocksUpdate((ClientPacketListener) (Object) this, packet);
    }

    @Inject(method = "handleForgetLevelChunk", at = @At("TAIL"))
    private void ghostsync$afterForgetLevelChunk(ClientboundForgetLevelChunkPacket packet, CallbackInfo ci) {
        Minecraft26PacketAdapter.afterForgetLevelChunk((ClientPacketListener) (Object) this, packet);
    }

    @Inject(method = "handleBlockChangedAck", at = @At("TAIL"))
    private void ghostsync$afterBlockChangedAck(ClientboundBlockChangedAckPacket packet, CallbackInfo ci) {
        Minecraft26PacketAdapter.afterBlockChangedAck(packet);
    }

    @Inject(method = "handleContainerSetSlot", at = @At("TAIL"))
    private void ghostsync$afterContainerSlot(ClientboundContainerSetSlotPacket packet, CallbackInfo ci) {
        Minecraft26PacketAdapter.afterContainerSlot(packet);
    }

    @Inject(method = "handleContainerContent", at = @At("TAIL"))
    private void ghostsync$afterContainerContent(ClientboundContainerSetContentPacket packet, CallbackInfo ci) {
        Minecraft26PacketAdapter.afterContainerContent(packet);
    }

    @Inject(method = "handleSetPlayerInventory", at = @At("TAIL"))
    private void ghostsync$afterPlayerInventory(ClientboundSetPlayerInventoryPacket packet, CallbackInfo ci) {
        Minecraft26PacketAdapter.afterPlayerInventory(packet);
    }
}
