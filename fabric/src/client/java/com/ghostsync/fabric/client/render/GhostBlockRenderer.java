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
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector4f;

/**
 * Temporary depth-tested white overlay for confirmed block ghosts.
 *
 * <p>This deliberately does not implement the transparency slider by drawing
 * through walls. True block transparency is implemented at terrain tessellation
 * time so the original textured model itself becomes translucent.</p>
 */
public final class GhostBlockRenderer {
    private static final Vector4f COLOR_MODULATOR = new Vector4f(1.0f, 1.0f, 1.0f, 1.0f);
    private static final Vector3f MODEL_OFFSET = new Vector3f();
    private static final Matrix4f TEXTURE_MATRIX = new Matrix4f();
    private static final StagedVertexBuffer VISIBLE_BUFFER = new StagedVertexBuffer(
            () -> "Ghost Sync visible block overlay", RenderType.SMALL_BUFFER_SIZE);

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

        List<BlockBox> boxes = new ArrayList<>();
        for (BlockKey key : GhostSyncRuntime.DETECTION.confirmedGhostBlocks()) {
            if (key.connectionEpoch() != connectionEpoch
                    || key.worldEpoch() != worldEpoch
                    || !key.dimensionId().equals(dimensionId)) continue;
            long dx = (long) key.x() - playerX;
            long dz = (long) key.z() - playerZ;
            if (dx * dx + dz * dz > maxDistanceSquared) continue;
            boxes.add(new BlockBox(key.x(), key.y(), key.z()));
        }

        frameState = new FrameState(List.copyOf(boxes), (float) config.blocks.overlayStrength);
    }

    private static void render(LevelRenderContext context) {
        FrameState state = frameState;
        if (state.boxes().isEmpty() || state.overlayAlpha() <= 0.0f) return;
        renderLayer(context, RenderPipelines.DEBUG_FILLED_BOX, VISIBLE_BUFFER, state, state.overlayAlpha());
    }

    private static void renderLayer(
            LevelRenderContext context,
            RenderPipeline pipeline,
            StagedVertexBuffer stagedBuffer,
            FrameState state,
            float alpha) {
        VertexFormat format = pipeline.getVertexFormatBinding(0);
        if (format == null) return;
        PrimitiveTopology primitive = pipeline.getPrimitiveTopology();
        StagedVertexBuffer.Draw stagedDraw = stagedBuffer.appendDraw(
                format,
                primitive,
                primitive == PrimitiveTopology.QUADS
                        ? RenderSystem.getProjectionType().vertexSorting()
                        : null);

        PoseStack poseStack = context.poseStack();
        Vec3 camera = context.levelState().cameraRenderState.pos;
        poseStack.pushPose();
        poseStack.translate(-camera.x, -camera.y, -camera.z);
        VertexConsumer builder = stagedBuffer.getVertexBuilder(stagedDraw);
        Matrix4fc matrix = poseStack.last().pose();
        for (BlockBox box : state.boxes()) addFilledBox(matrix, builder, box.x(), box.y(), box.z(), alpha);
        poseStack.popPose();

        stagedBuffer.upload();
        StagedVertexBuffer.ExecuteInfo info = stagedBuffer.getExecuteInfo(stagedDraw);
        if (info != null) executeDraw(Minecraft.getInstance(), info, pipeline);
        stagedBuffer.endFrame();
    }

    private static void addFilledBox(Matrix4fc matrix, VertexConsumer buffer, float x, float y, float z, float alpha) {
        float maxX = x + 1.0f;
        float maxY = y + 1.0f;
        float maxZ = z + 1.0f;
        addQuad(buffer, matrix, x, y, maxZ, maxX, y, maxZ, maxX, maxY, maxZ, x, maxY, maxZ, alpha);
        addQuad(buffer, matrix, maxX, y, z, x, y, z, x, maxY, z, maxX, maxY, z, alpha);
        addQuad(buffer, matrix, x, y, z, x, y, maxZ, x, maxY, maxZ, x, maxY, z, alpha);
        addQuad(buffer, matrix, maxX, y, maxZ, maxX, y, z, maxX, maxY, z, maxX, maxY, maxZ, alpha);
        addQuad(buffer, matrix, x, maxY, maxZ, maxX, maxY, maxZ, maxX, maxY, z, x, maxY, z, alpha);
        addQuad(buffer, matrix, x, y, z, maxX, y, z, maxX, y, maxZ, x, y, maxZ, alpha);
    }

    private static void addQuad(
            VertexConsumer buffer,
            Matrix4fc matrix,
            float x1, float y1, float z1,
            float x2, float y2, float z2,
            float x3, float y3, float z3,
            float x4, float y4, float z4,
            float alpha) {
        buffer.addVertex(matrix, x1, y1, z1).setColor(1.0f, 1.0f, 1.0f, alpha);
        buffer.addVertex(matrix, x2, y2, z2).setColor(1.0f, 1.0f, 1.0f, alpha);
        buffer.addVertex(matrix, x3, y3, z3).setColor(1.0f, 1.0f, 1.0f, alpha);
        buffer.addVertex(matrix, x4, y4, z4).setColor(1.0f, 1.0f, 1.0f, alpha);
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
                        () -> "Ghost Sync confirmed block overlay",
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

    private record BlockBox(int x, int y, int z) {}
    private record FrameState(List<BlockBox> boxes, float overlayAlpha) {
        private static final FrameState EMPTY = new FrameState(List.of(), 0.0f);
    }
}
