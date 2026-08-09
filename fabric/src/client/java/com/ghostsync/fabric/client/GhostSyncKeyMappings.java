package com.ghostsync.fabric.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

/** Registers the manual detection refresh key. */
public final class GhostSyncKeyMappings {
    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath("ghostsync", "controls"));

    private static final KeyMapping REFRESH = KeyBindingHelper.registerKeyBinding(
            new KeyMapping(
                    "key.ghostsync.refresh",
                    InputConstants.Type.KEYSYM,
                    InputConstants.KEY_K,
                    CATEGORY));

    private GhostSyncKeyMappings() {
    }

    public static void initialize() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (REFRESH.consumeClick()) {
                GhostSyncRuntime.requestRefresh();
            }
        });
    }
}
