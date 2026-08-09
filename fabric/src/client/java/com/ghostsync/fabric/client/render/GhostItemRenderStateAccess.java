package com.ghostsync.fabric.client.render;

/** Mixin-backed visual marker carried from GUI extraction into deferred item rendering. */
public interface GhostItemRenderStateAccess {
    float ghostsync$getAlpha();

    void ghostsync$setAlpha(float alpha);

    float ghostsync$getOverlayAlpha();

    void ghostsync$setOverlayAlpha(float alpha);
}
