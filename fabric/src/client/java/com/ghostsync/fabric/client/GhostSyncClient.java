package com.ghostsync.fabric.client;

import net.fabricmc.api.ClientModInitializer;

/**
 * Minecraft/Fabric adapter entry point.
 *
 * <p>Milestone 1 intentionally performs no packet interception or rendering.
 * Those features will be layered on only after the version-independent core is
 * validated.</p>
 */
public final class GhostSyncClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // Intentionally empty for Milestone 1.
    }
}
