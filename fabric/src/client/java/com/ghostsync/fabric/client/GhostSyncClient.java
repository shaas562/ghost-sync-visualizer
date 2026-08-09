package com.ghostsync.fabric.client;

import com.ghostsync.fabric.client.config.GhostSyncConfigManager;
import net.fabricmc.api.ClientModInitializer;

/**
 * Minecraft/Fabric adapter entry point.
 */
public final class GhostSyncClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        GhostSyncConfigManager.load();
        GhostSyncKeyMappings.initialize();
    }
}
