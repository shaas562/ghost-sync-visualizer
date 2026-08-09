package com.ghostsync.fabric.client.mixin;

import com.ghostsync.fabric.client.render.GhostTerrainState;
import com.mojang.blaze3d.vertex.QuadInstance;
import net.minecraft.client.renderer.block.BlockQuadOutput;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.SectionCompiler;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Changes only confirmed ghost geometry during normal terrain tessellation.
 * The original texture/UV/model geometry is preserved; only the terrain layer
 * and per-vertex alpha are changed.
 */
@Mixin(SectionCompiler.class)
abstract class SectionCompilerMixin {
    @Redirect(
            method = "compile",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/block/ModelBlockRenderer;tesselateBlock(Lnet/minecraft/client/renderer/block/BlockQuadOutput;FFFLnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/client/renderer/block/dispatch/BlockStateModel;J)V"))
    private void ghostsync$tessellateGhostTransparency(
            ModelBlockRenderer renderer,
            BlockQuadOutput output,
            float x,
            float y,
            float z,
            BlockAndTintGetter level,
            BlockPos pos,
            BlockState state,
            BlockStateModel model,
            long seed) {
        float alpha = GhostTerrainState.modelAlpha(pos);
        if (alpha >= 0.9999f) {
            renderer.tesselateBlock(output, x, y, z, level, pos, state, model, seed);
            return;
        }

        if (alpha <= 0.0001f) {
            // Fully transparent means no original textured geometry is needed.
            return;
        }

        int alphaByte = Math.clamp(Math.round(alpha * 255.0f), 0, 255);
        int alphaMultiplier = (alphaByte << 24) | 0x00FFFFFF;

        BlockQuadOutput ghostOutput = (quadX, quadY, quadZ, quad, instance) -> {
            instance.multiplyColor(alphaMultiplier);
            output.put(quadX, quadY, quadZ, translucentCopy(quad), instance);
        };

        renderer.tesselateBlock(ghostOutput, x, y, z, level, pos, state, model, seed);
    }

    private static BakedQuad translucentCopy(BakedQuad quad) {
        BakedQuad.MaterialInfo material = quad.materialInfo();
        if (material.layer() == ChunkSectionLayer.TRANSLUCENT) return quad;

        BakedQuad.MaterialInfo translucentMaterial = new BakedQuad.MaterialInfo(
                material.sprite(),
                ChunkSectionLayer.TRANSLUCENT,
                material.itemRenderType(),
                material.tintIndex(),
                material.shade(),
                material.lightEmission());

        return new BakedQuad(
                quad.position0(),
                quad.position1(),
                quad.position2(),
                quad.position3(),
                quad.packedUV0(),
                quad.packedUV1(),
                quad.packedUV2(),
                quad.packedUV3(),
                quad.direction(),
                translucentMaterial);
    }
}
