package com.ghostsync.fabric.client.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import java.util.Optional;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/** Builds a GUI pipeline identical to vanilla's item blit except for the fragment shader. */
public final class GhostItemSilhouettePipeline {
    public static final RenderPipeline PIPELINE = create();

    private GhostItemSilhouettePipeline() {}

    private static RenderPipeline create() {
        RenderPipeline base = RenderPipelines.GUI_TEXTURED_PREMULTIPLIED_ALPHA;
        RenderPipeline.Snippet clone = new RenderPipeline.Snippet(
                Optional.of(base.getVertexShader()),
                Optional.of(base.getFragmentShader()),
                Optional.of(base.getShaderDefines()),
                Optional.of(base.getBindGroupLayouts()),
                base.getColorTargetStates(),
                base.getColorTargetStates().length,
                Optional.of(base.getDepthStencilState()),
                Optional.of(base.getPolygonMode()),
                Optional.of(base.isCull()),
                base.getVertexFormatBindings(),
                Optional.of(base.getPrimitiveTopology()));

        return RenderPipeline.builder(clone)
                .withLocation(Identifier.fromNamespaceAndPath("ghostsync", "pipeline/item_white_silhouette"))
                .withFragmentShader(Identifier.fromNamespaceAndPath("ghostsync", "core/item_white_silhouette"))
                .build();
    }
}
