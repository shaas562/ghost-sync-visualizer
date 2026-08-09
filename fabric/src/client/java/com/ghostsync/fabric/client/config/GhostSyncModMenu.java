package com.ghostsync.fabric.client.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/** Optional Mod Menu integration. The mod itself does not require Mod Menu. */
public final class GhostSyncModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return GhostSyncConfigScreen::new;
    }
}
