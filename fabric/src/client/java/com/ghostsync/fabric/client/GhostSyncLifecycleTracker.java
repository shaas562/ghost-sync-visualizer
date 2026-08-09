package com.ghostsync.fabric.client;

import com.ghostsync.core.Presence;
import com.ghostsync.core.SlotKey;
import com.ghostsync.fabric.client.config.GhostSyncConfigManager;
import java.util.HashMap;
import java.util.Map;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

/**
 * Tracks connection/world/container lifecycles and observes local inventory
 * changes without needing to fake any server interaction.
 */
public final class GhostSyncLifecycleTracker {
    private static final Map<Integer, Presence> PREVIOUS_MENU_PRESENCE = new HashMap<>();
    private static final Map<Integer, Presence> PREVIOUS_PLAYER_INVENTORY_PRESENCE = new HashMap<>();

    private static ClientLevel previousLevel;
    private static AbstractContainerMenu previousMenu;
    private static long previousMenuEpoch;

    private GhostSyncLifecycleTracker() {
    }

    public static void initialize() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            resetLocalSnapshots();
            GhostSyncRuntime.beginConnection();
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            resetLocalSnapshots();
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
            PREVIOUS_MENU_PRESENCE.clear();
            PREVIOUS_PLAYER_INVENTORY_PRESENCE.clear();
            return;
        }

        AbstractContainerMenu currentMenu = client.player.containerMenu;
        if (currentMenu != previousMenu) {
            forgetPreviousMenu();
            previousMenu = currentMenu;
            previousMenuEpoch = GhostSyncRuntime.containerEpoch(currentMenu);
            PREVIOUS_MENU_PRESENCE.clear();
        }

        if (GhostSyncConfigManager.current().shouldDetectItems()) {
            observeCurrentMenu(currentMenu, previousMenuEpoch);
            observePlayerInventory(client.player.getInventory());
        }
    }

    private static void observeCurrentMenu(AbstractContainerMenu menu, long menuEpoch) {
        for (int slotIndex = 0; slotIndex < menu.slots.size(); slotIndex++) {
            Presence presence = menu.getSlot(slotIndex).getItem().isEmpty()
                    ? Presence.ABSENT
                    : Presence.PRESENT;
            Presence previous = PREVIOUS_MENU_PRESENCE.put(slotIndex, presence);
            if (previous == presence) {
                continue;
            }

            SlotKey key = new SlotKey(
                    GhostSyncRuntime.connectionEpoch(),
                    menuEpoch,
                    menu.containerId,
                    slotIndex);
            GhostSyncRuntime.DETECTION.slots().observeClientState(key, presence);
        }
    }

    private static void observePlayerInventory(Inventory inventory) {
        for (int slotIndex = 0; slotIndex < inventory.getContainerSize(); slotIndex++) {
            Presence presence = inventory.getItem(slotIndex).isEmpty()
                    ? Presence.ABSENT
                    : Presence.PRESENT;
            Presence previous = PREVIOUS_PLAYER_INVENTORY_PRESENCE.put(slotIndex, presence);
            if (previous == presence) {
                continue;
            }
            GhostSyncRuntime.DETECTION.slots().observeClientState(
                    GhostSyncRuntime.playerInventoryKey(slotIndex), presence);
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
        PREVIOUS_MENU_PRESENCE.clear();
    }

    private static void resetLocalSnapshots() {
        previousLevel = null;
        previousMenu = null;
        previousMenuEpoch = 0;
        PREVIOUS_MENU_PRESENCE.clear();
        PREVIOUS_PLAYER_INVENTORY_PRESENCE.clear();
    }
}
