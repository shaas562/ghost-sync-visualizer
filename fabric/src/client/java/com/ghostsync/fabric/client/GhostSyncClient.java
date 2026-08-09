package com.ghostsync.fabric.client;

import com.ghostsync.fabric.client.config.GhostSyncConfigManager;
import com.ghostsync.fabric.client.render.GhostBlockRenderer;
import net.fabricmc.api.ClientModInitializer;

/** Minecraft/Fabric adapter entry point. */
public final class GhostSyncClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        GhostSyncConfigManager.load();
        GhostSyncLifecycleTracker.initialize();
        GhostSyncBlockInteractionTracker.initialize();
        GhostSyncKeyMappings.initialize();
        GhostBlockRenderer.initialize();
    }
}
