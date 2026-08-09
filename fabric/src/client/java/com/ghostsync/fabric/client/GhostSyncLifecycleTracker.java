package com.ghostsync.fabric.client;

import com.ghostsync.core.Presence;
import com.ghostsync.core.SlotKey;
import com.ghostsync.fabric.client.config.GhostSyncConfig;
import com.ghostsync.fabric.client.config.GhostSyncConfigManager;
import com.ghostsync.fabric.client.render.GhostTerrainState;
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
    private static boolean blockDetectionEnabled;
    private static boolean itemDetectionEnabled;

    private GhostSyncLifecycleTracker() {}

    public static void initialize() {
        GhostSyncConfig config = GhostSyncConfigManager.current();
        blockDetectionEnabled = config.shouldDetectBlocks();
        itemDetectionEnabled = config.shouldDetectItems();

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            resetLocalSnapshots();
            GhostTerrainState.discardForWorldChange();
            GhostSyncRuntime.beginConnection();
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            resetLocalSnapshots();
            GhostTerrainState.discardForWorldChange();
            GhostSyncRuntime.endConnection();
        });

        ClientTickEvents.END_CLIENT_TICK.register(GhostSyncLifecycleTracker::onEndTick);
    }

    private static void onEndTick(Minecraft client) {
        GhostSyncConfig config = GhostSyncConfigManager.current();
        applyDetectionEnablement(config);

        ClientLevel currentLevel = client.level;
        if (currentLevel != previousLevel) {
            GhostTerrainState.discardForWorldChange();
            if (currentLevel != null) GhostSyncRuntime.beginWorld();
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

        if (config.shouldDetectItems()) {
            observeCurrentMenu(currentMenu);
            observePlayerInventory(client.player.getInventory());
        }
    }

    /**
     * Turning a detector off is a certainty boundary. Its old confirmations are
     * discarded immediately so re-enabling cannot resurrect stale ghosts without
     * fresh server-authoritative evidence.
     */
    private static void applyDetectionEnablement(GhostSyncConfig config) {
        boolean detectBlocks = config.shouldDetectBlocks();
        boolean detectItems = config.shouldDetectItems();

        if (blockDetectionEnabled && !detectBlocks) {
            GhostSyncRuntime.DETECTION.resetBlockState();
        }
        if (itemDetectionEnabled && !detectItems) {
            GhostSyncRuntime.DETECTION.resetSlotState();
        }

        blockDetectionEnabled = detectBlocks;
        itemDetectionEnabled = detectItems;
    }

    private static void observeCurrentMenu(AbstractContainerMenu menu) {
        for (int slotIndex = 0; slotIndex < menu.slots.size(); slotIndex++) {
            // Player inventory is observed once through its stable connection-scoped
            // identity below. Do not create a second container-scoped key for it.
            if (GhostSlotKeys.isPlayerInventoryBacked(menu, slotIndex)) continue;

            Presence presence = menu.getSlot(slotIndex).getItem().isEmpty()
                    ? Presence.ABSENT
                    : Presence.PRESENT;
            Presence previous = PREVIOUS_MENU_PRESENCE.put(slotIndex, presence);
            if (previous == presence) continue;

            SlotKey key = GhostSlotKeys.forMenuSlot(menu, slotIndex);
            GhostSyncRuntime.DETECTION.slots().observeClientState(key, presence);
        }
    }

    private static void observePlayerInventory(Inventory inventory) {
        for (int slotIndex = 0; slotIndex < inventory.getContainerSize(); slotIndex++) {
            Presence presence = inventory.getItem(slotIndex).isEmpty()
                    ? Presence.ABSENT
                    : Presence.PRESENT;
            Presence previous = PREVIOUS_PLAYER_INVENTORY_PRESENCE.put(slotIndex, presence);
            if (previous == presence) continue;
            GhostSyncRuntime.DETECTION.slots().observeClientState(
                    GhostSyncRuntime.playerInventoryKey(slotIndex), presence);
        }
    }

    private static void forgetPreviousMenu() {
        if (previousMenu == null) return;
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
