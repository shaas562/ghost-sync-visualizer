package com.ghostsync.fabric;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Runs under Fabric Loader's official fabric-loader-junit launcher, which starts
 * a headless Knot client classloader and Mixin environment without invoking the
 * Minecraft client main method or creating a graphics window.
 */
final class HeadlessMixinBootstrapProbe {
    @Test
    void productionMixinsAndTargetsResolveInsideHeadlessKnot() throws Exception {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        assertNotNull(loader);

        String config;
        try (InputStream input = loader.getResourceAsStream("ghostsync.client.mixins.json")) {
            assertNotNull(input, "ghostsync.client.mixins.json is missing from the Knot runtime classpath");
            config = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }

        List<String> mixins = List.of(
                "com.ghostsync.fabric.client.mixin.AbstractContainerScreenMixin",
                "com.ghostsync.fabric.client.mixin.BlockStatePredictionHandlerMixin",
                "com.ghostsync.fabric.client.mixin.ClientLevelMixin",
                "com.ghostsync.fabric.client.mixin.ClientPacketListenerMixin",
                "com.ghostsync.fabric.client.mixin.GameRendererMixin",
                "com.ghostsync.fabric.client.mixin.GuiRendererMixin",
                "com.ghostsync.fabric.client.mixin.SectionCompilerMixin",
                "com.ghostsync.fabric.client.mixin.TrackingItemStackRenderStateMixin");

        List<String> targets = List.of(
                "net.minecraft.client.multiplayer.ClientPacketListener",
                "net.minecraft.client.multiplayer.ClientLevel",
                "net.minecraft.client.gui.screens.inventory.AbstractContainerScreen",
                "net.minecraft.client.gui.render.GuiRenderer",
                "net.minecraft.client.renderer.chunk.SectionCompiler",
                "net.minecraft.client.multiplayer.prediction.BlockStatePredictionHandler",
                "net.minecraft.client.renderer.item.TrackingItemStackRenderState");

        for (String mixin : mixins) {
            String simpleName = mixin.substring(mixin.lastIndexOf('.') + 1);
            assertTrue(config.contains('"' + simpleName + '"'), "Mixin is not registered: " + simpleName);
            assertNotNull(Class.forName(mixin, false, loader), "Mixin class did not resolve: " + mixin);
        }

        for (String target : targets) {
            assertNotNull(Class.forName(target, false, loader), "Minecraft target did not resolve: " + target);
        }
    }
}
