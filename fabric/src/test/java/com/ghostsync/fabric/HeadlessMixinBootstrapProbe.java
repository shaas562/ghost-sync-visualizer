package com.ghostsync.fabric;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.Mixins;

/**
 * Headless smoke probe for the runtime classpath and Mixin configuration.
 *
 * <p>This deliberately does not initialize Minecraft or create a window. It
 * proves that the Mixin subsystem can bootstrap, the production config is on
 * the runtime classpath, and both target/mixin classes can be resolved from the
 * exact Loom test runtime.</p>
 */
public final class HeadlessMixinBootstrapProbe {
    private HeadlessMixinBootstrapProbe() {}

    public static void main(String[] args) throws Exception {
        initializeFabricGlobalPropertiesForHeadlessLaunch();
        MixinBootstrap.init();
        Mixins.addConfiguration("ghostsync.client.mixins.json");

        String config;
        try (InputStream input = HeadlessMixinBootstrapProbe.class.getClassLoader()
                .getResourceAsStream("ghostsync.client.mixins.json")) {
            if (input == null) {
                throw new IllegalStateException("ghostsync.client.mixins.json is missing from the runtime classpath");
            }
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

        ClassLoader loader = HeadlessMixinBootstrapProbe.class.getClassLoader();
        for (String mixin : mixins) {
            if (!config.contains('"' + mixin.substring(mixin.lastIndexOf('.') + 1) + '"')) {
                throw new IllegalStateException("Mixin is not registered in config: " + mixin);
            }
            Class.forName(mixin, false, loader);
        }
        for (String target : targets) {
            Class.forName(target, false, loader);
        }

        System.out.println("Headless Mixin bootstrap/class-loading probe passed.");
    }

    /**
     * Fabric's Mixin global-property service normally receives this map from the
     * Knot launcher before Mixin starts. A plain Gradle JavaExec has no Knot
     * launcher, so initialize only that exact launcher-owned property map. This
     * uses Fabric Loader's own testing-visible state rather than mocking Mixin.
     */
    private static void initializeFabricGlobalPropertiesForHeadlessLaunch() throws Exception {
        Class<?> launcherBase = Class.forName("net.fabricmc.loader.impl.launch.FabricLauncherBase");
        Method setProperties = launcherBase.getDeclaredMethod("setProperties", java.util.Map.class);
        setProperties.setAccessible(true);
        setProperties.invoke(null, new HashMap<String, Object>());
    }
}
