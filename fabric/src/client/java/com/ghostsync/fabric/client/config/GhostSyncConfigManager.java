package com.ghostsync.fabric.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Loads, validates, migrates and atomically saves the client configuration. */
public final class GhostSyncConfigManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("GhostSync");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("ghostsync.json");

    private static GhostSyncConfig current = new GhostSyncConfig();

    private GhostSyncConfigManager() {
    }

    public static synchronized GhostSyncConfig load() {
        GhostSyncConfig loaded = null;
        if (Files.isRegularFile(CONFIG_PATH)) {
            try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                loaded = GSON.fromJson(reader, GhostSyncConfig.class);
            } catch (Exception exception) {
                LOGGER.warn("Could not read Ghost Sync configuration; using defaults", exception);
            }
        }

        if (loaded == null) {
            loaded = new GhostSyncConfig();
        }
        migrate(loaded);
        loaded.normalize();
        current = loaded;
        return current;
    }

    public static synchronized GhostSyncConfig current() {
        return current;
    }

    public static synchronized void save() {
        current.normalize();
        Path parent = CONFIG_PATH.getParent();
        Path temp = CONFIG_PATH.resolveSibling(CONFIG_PATH.getFileName() + ".tmp");
        try {
            Files.createDirectories(parent);
            try (Writer writer = Files.newBufferedWriter(temp)) {
                GSON.toJson(current, writer);
            }
            try {
                Files.move(temp, CONFIG_PATH,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException atomicMoveFailure) {
                Files.move(temp, CONFIG_PATH, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            LOGGER.error("Could not save Ghost Sync configuration", exception);
            try {
                Files.deleteIfExists(temp);
            } catch (IOException ignored) {
                // Preserve the original save failure as the useful error.
            }
        }
    }

    private static void migrate(GhostSyncConfig config) {
        // Schema 1 is the first persisted version. Future migrations are applied
        // here before normalize() updates the schema marker.
        if (config.schemaVersion < 1) {
            config.schemaVersion = 1;
        }
    }
}
