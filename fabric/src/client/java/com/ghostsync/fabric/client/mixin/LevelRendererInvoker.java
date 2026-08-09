package com.ghostsync.fabric.client.mixin;

import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Narrow bridge to rebuild only terrain sections affected by ghost alpha changes. */
@Mixin(LevelRenderer.class)
public interface LevelRendererInvoker {
    @Invoker("setSectionDirty")
    void ghostsync$setSectionDirty(int sectionX, int sectionY, int sectionZ);
}
