package com.ghostsync.fabric.client.render;

import com.ghostsync.core.BlockKey;
import com.ghostsync.fabric.client.GhostSyncRuntime;
import com.ghostsync.fabric.client.config.GhostSyncConfig;
import com.ghostsync.fabric.client.config.GhostSyncConfigManager;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.StagedVertexBuffer;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.joml.Vector4f;

/**
 * Depth-tested, model-shaped white overlay for confirmed block ghosts.
 *
 * <p>The overlay reuses the client's currently baked block model, so resource
 * packs, multipart models and most modded block models retain their real shape.
 * Original-texture transparency is handled separately during terrain
 * tessellation; it is never approximated with a through-wall pass.</p>
 */
public final class GhostBlockRenderer {
    private static final Vector4f COLOR_MODULATOR = new Vector4f(1.0f, 1.0f, 1.0f, 1.0f);
    private static final Vector3f MODEL_OFFSET = new Vector3f();
    private static final Matrix4f TEXTURE_MATRIX = new Matrix4f();
    private static final StagedVertexBuffer VISIBLE_BUFFER = new StagedVertexBuffer(
            () -> "Ghost Sync model overlay", RenderType.SMALL_BUFFER_SIZE);

    private static volatile FrameState frameState = FrameState.EMPTY;
    private static boolean initialized;

    private GhostBlockRenderer() {}

    public static synchronized void initialize() {
        if (initialized) return;
        initialized = true;
        LevelExtractionEvents.END_EXTRACTION.register(GhostBlockRenderer::extract);
        LevelRenderEvents.AFTER_TRANSLUCENT_TERRAIN.register(GhostBlockRenderer::render);
    }

    private static void extract(LevelExtractionContext context) {
        Minecraft client = Minecraft.getInstance();
        GhostSyncConfig config = GhostSyncConfigManager.current();
        if (!config.shouldDetectBlocks()
                || config.blocks.overlayStrength <= 0.0
                || client.level == null
                || client.player == null) {
            frameState = FrameState.EMPTY;
            return;
        }

        long connectionEpoch = GhostSyncRuntime.connectionEpoch();
        long worldEpoch = GhostSyncRuntime.worldEpoch();
        String dimensionId = client.level.dimension().identifier().toString();
        int playerX = client.player.blockPosition().getX();
        int playerZ = client.player.blockPosition().getZ();
        long maxDistance = (long) config.blocks.detectionDistanceChunks * 16L;
        long maxDistanceSquared = maxDistance * maxDistance;

        List<ModelQuad> quads = new ArrayList<>();
        for (BlockKey key : GhostSyncRuntime.DETECTION.confirmedGhostBlocks()) {
            if (key.connectionEpoch() != connectionEpoch
                    || key.worldEpoch() != worldEpoch
                    || !key.dimensionId().equals(dimensionId)) continue;

            long dx = (long) key.x() - playerX;
            long dz = (long) key.z() - playerZ;
            if (dx * dx + dz * dz > maxDistanceSquared) continue;

            BlockPos pos = new BlockPos(key.x(), key.y(), key.z());
            if (!client.level.hasChunkAt(pos)) continue;
            BlockState state = client.level.getBlockState(pos);
            if (state.isAir() || state.getRenderShape() != RenderShape.MODEL) continue;

            BlockStateModel model = client.getModelManager().getBlockStateModelSet().get(state);
            RandomSource random = RandomSource.create(state.getSeed(pos));
            List<BlockStateModelPart> parts = new ArrayList<>();
            model.collectParts(random, parts);

            for (BlockStateModelPart part : parts) {
                addPartQuads(quads, part, pos, null);
                for (Direction direction : Direction.values()) {
                    addPartQuads(quads, part, pos, direction);
                }
            }
        }

        frameState = new FrameState(List.copyOf(quads), (float) config.blocks.overlayStrength);
    }

    private static void addPartQuads(
            List<ModelQuad> output,
            BlockStateModelPart part,
            BlockPos pos,
            Direction direction) {
        for (BakedQuad quad : part.getQuads(direction)) {
            output.add(new ModelQuad(
                    worldVertex(pos, quad.position0()),
                    worldVertex(pos, quad.position1()),
                    worldVertex(pos, quad.position2()),
                    worldVertex(pos, quad.position3())));
        }
    }

    private static Vertex worldVertex(BlockPos pos, Vector3fc vertex) {
        return new Vertex(
                pos.getX() + vertex.x(),
                pos.getY() + vertex.y(),
                pos.getZ() + vertex.z());
    }

    private static void render(LevelRenderContext context) {
        FrameState state = frameState;
        if (state.quads().isEmpty() || state.overlayAlpha() <= 0.0f) return;

        RenderPipeline pipeline = RenderPipelines.DEBUG_FILLED_BOX;
        VertexFormat format = pipeline.getVertexFormatBinding(0);
        if (format == null) return;
        PrimitiveTopology primitive = pipeline.getPrimitiveTopology();
        StagedVertexBuffer.Draw stagedDraw = VISIBLE_BUFFER.appendDraw(
                format,
                primitive,
                primitive == PrimitiveTopology.QUADS
                        ? RenderSystem.getProjectionType().vertexSorting()
                        : null);

        PoseStack poseStack = context.poseStack();
        Vec3 camera = context.levelState().cameraRenderState.pos;
        poseStack.pushPose();
        poseStack.translate(-camera.x, -camera.y, -camera.z);
        Matrix4fc matrix = poseStack.last().pose();
        VertexConsumer builder = VISIBLE_BUFFER.getVertexBuilder(stagedDraw);
        for (ModelQuad quad : state.quads()) {
            addQuad(matrix, builder, quad, state.overlayAlpha());
        }
        poseStack.popPose();

        VISIBLE_BUFFER.upload();
        StagedVertexBuffer.ExecuteInfo info = VISIBLE_BUFFER.getExecuteInfo(stagedDraw);
        if (info != null) executeDraw(Minecraft.getInstance(), info, pipeline);
        VISIBLE_BUFFER.endFrame();
    }

    private static void addQuad(
            Matrix4fc matrix,
            VertexConsumer buffer,
            ModelQuad quad,
            float alpha) {
        addVertex(matrix, buffer, quad.a(), alpha);
        addVertex(matrix, buffer, quad.b(), alpha);
        addVertex(matrix, buffer, quad.c(), alpha);
        addVertex(matrix, buffer, quad.d(), alpha);
    }

    private static void addVertex(Matrix4fc matrix, VertexConsumer buffer, Vertex vertex, float alpha) {
        buffer.addVertex(matrix, vertex.x(), vertex.y(), vertex.z())
                .setColor(1.0f, 1.0f, 1.0f, alpha);
    }

    private static void executeDraw(Minecraft client, StagedVertexBuffer.ExecuteInfo info, RenderPipeline pipeline) {
        GpuBufferSlice transforms = RenderSystem.getDynamicUniforms().writeTransform(
                RenderSystem.getModelViewMatrixCopy(), COLOR_MODULATOR, MODEL_OFFSET, TEXTURE_MATRIX);
        RenderTarget target = client.gameRenderer.mainRenderTarget();
        GpuTextureView color = target.getColorTextureView();
        if (color == null) return;

        try (RenderPass renderPass = RenderSystem.getDevice()
                .createCommandEncoder()
                .createRenderPass(
                        () -> "Ghost Sync confirmed model overlay",
                        color,
                        Optional.empty(),
                        target.getDepthTextureView(),
                        OptionalDouble.empty())) {
            renderPass.setPipeline(pipeline);
            RenderSystem.bindDefaultUniforms(renderPass);
            renderPass.setUniform("DynamicTransforms", transforms);
            renderPass.setVertexBuffer(0, info.vertexBuffer().slice());
            renderPass.setIndexBuffer(info.indexBuffer(), info.indexType());
            renderPass.drawIndexed(info.indexCount(), 1, info.firstIndex(), info.baseVertex(), 0);
        }
    }

    public static void close() {
        VISIBLE_BUFFER.close();
    }

    private record Vertex(float x, float y, float z) {}
    private record ModelQuad(Vertex a, Vertex b, Vertex c, Vertex d) {}
    private record FrameState(List<ModelQuad> quads, float overlayAlpha) {
        private static final FrameState EMPTY = new FrameState(List.of(), 0.0f);
    }
}
