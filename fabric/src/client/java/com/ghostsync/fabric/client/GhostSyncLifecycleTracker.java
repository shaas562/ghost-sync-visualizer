package com.ghostsync.fabric.client;

import com.ghostsync.core.Presence;
import com.ghostsync.core.SlotKey;
import com.ghostsync.fabric.client.config.GhostSyncConfigManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.inventory.AbstractContainerMenu;

/**
 * Tracks connection/world/container lifecycles and observes local inventory
 * changes without needing to fake any server interaction.
 */
public final class GhostSyncLifecycleTracker {
    private static ClientLevel previousLevel;
    private static AbstractContainerMenu previousMenu;
    private static long previousMenuEpoch;

    private GhostSyncLifecycleTracker() {
    }

    public static void initialize() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            previousLevel = null;
            previousMenu = null;
            previousMenuEpoch = 0;
            GhostSyncRuntime.beginConnection();
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            previousLevel = null;
            previousMenu = null;
            previousMenuEpoch = 0;
            GhostSyncRuntime.endConnection();
        });

        ClientTickEvents.END_CLIENT_TICK.register(GhostSyncLifecycleTracker::onEndTick);
    }

    private static void onEndTick(Minecraft client) {
        ClientLevel currentLevel = client.level;
        if (currentLevel != previousLevel) {
            if (currentLevel != null) {
                GhostSyncRuntime.beginWorld();
            }
            previousLevel = currentLevel;
        }

        if (client.player == null) {
            previousMenu = null;
            previousMenuEpoch = 0;
            return;
        }

        AbstractContainerMenu currentMenu = client.player.containerMenu;
        if (currentMenu != previousMenu) {
            forgetPreviousMenu();
            previousMenu = currentMenu;
            previousMenuEpoch = GhostSyncRuntime.containerEpoch(currentMenu);
        }

        if (GhostSyncConfigManager.current().shouldDetectItems()) {
            observeCurrentMenu(currentMenu, previousMenuEpoch);
        }
    }

    private static void observeCurrentMenu(AbstractContainerMenu menu, long menuEpoch) {
        for (int slotIndex = 0; slotIndex < menu.slots.size(); slotIndex++) {
            Presence presence = menu.getSlot(slotIndex).getItem().isEmpty()
                    ? Presence.ABSENT
                    : Presence.PRESENT;
            SlotKey key = new SlotKey(
                    GhostSyncRuntime.connectionEpoch(),
                    menuEpoch,
                    menu.containerId,
                    slotIndex);
            GhostSyncRuntime.DETECTION.slots().observeClientState(key, presence);
        }
    }

    private static void forgetPreviousMenu() {
        if (previousMenu == null) {
            return;
        }
        GhostSyncRuntime.DETECTION.closeContainer(
                GhostSyncRuntime.connectionEpoch(),
                previousMenuEpoch,
                previousMenu.containerId);
        GhostSyncRuntime.forgetContainer(previousMenu);
    }
}
