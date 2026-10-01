package dev.stya.blockzone.game.battlezone;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

@Mod.EventBusSubscriber(modid = "blockzone", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class BattlezoneClientRendering {
    private static final Logger LOGGER = LoggerFactory.getLogger(BattlezoneClientRendering.class);
    private static final int CIRCLE_SEGMENTS = 96;

    private static PostPass whiteoutPass;
    private static PostPass blitPass;
    private static TextureTarget scratchTarget;
    private static int targetWidth = -1;
    private static int targetHeight = -1;
    private static Matrix4f inverseProjection;
    private static Matrix4f inverseViewRotation;
    private static Vec3 cameraPosition;

    private BattlezoneClientRendering() {
    }

    @SubscribeEvent
    public static void registerReloadListener(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(new net.minecraft.server.packs.resources.SimplePreparableReloadListener<Void>() {
            @Override
            protected Void prepare(ResourceManager resourceManager, net.minecraft.util.profiling.ProfilerFiller profiler) {
                return null;
            }

            @Override
            protected void apply(Void ignored, ResourceManager resourceManager,
                                 net.minecraft.util.profiling.ProfilerFiller profiler) {
                releasePasses();
            }
        });
    }

    static void renderWorld(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        BattlezoneClientState.Snapshot state = BattlezoneClientState.current(event.getPartialTick());
        if (minecraft.level == null || state == null
                || !minecraft.level.dimension().location().equals(state.dimension())) {
            return;
        }

        cameraPosition = event.getCamera().getPosition();
        inverseProjection = new Matrix4f(event.getProjectionMatrix()).invert();
        Matrix4f viewRotation = new Matrix4f(event.getPoseStack().last().pose());
        viewRotation.m30(0.0F).m31(0.0F).m32(0.0F);
        inverseViewRotation = viewRotation.invert();

        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        AreaRenderer.renderMapBounds(state, poseStack, buffers);
        buffers.endBatch(RenderType.lines());
        renderCircleWall(state, poseStack);
        renderWhiteout(event.getPartialTick());
    }

    private static void renderWhiteout(float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        BattlezoneClientState.Snapshot state = BattlezoneClientState.current(partialTick);
        if (state == null || !state.whiteoutActive() || minecraft.level == null
                || !minecraft.level.dimension().location().equals(state.dimension())
                || inverseProjection == null || inverseViewRotation == null || cameraPosition == null) {
            return;
        }

        try {
            ensurePasses(minecraft);
            if (whiteoutPass == null || blitPass == null || scratchTarget == null) {
                return;
            }
            RenderTarget mainTarget = minecraft.getMainRenderTarget();
            double cameraX = cameraPosition.x;
            double cameraZ = cameraPosition.z;
            whiteoutPass.getEffect().safeGetUniform("InverseProjection").set(inverseProjection);
            whiteoutPass.getEffect().safeGetUniform("InverseViewRotation").set(inverseViewRotation);
            whiteoutPass.getEffect().safeGetUniform("CircleCenter").set(
                    (float) (state.centerX() - cameraX), (float) (state.centerZ() - cameraZ));
            whiteoutPass.getEffect().safeGetUniform("CircleRadius").set(state.radius());
            whiteoutPass.getEffect().safeGetUniform("MapBounds").set(
                    Math.min(state.x1(), state.x2()) - (float) cameraX,
                    Math.min(state.z1(), state.z2()) - (float) cameraZ,
                    Math.max(state.x1(), state.x2()) + 1.0F - (float) cameraX,
                    Math.max(state.z1(), state.z2()) + 1.0F - (float) cameraZ);

            whiteoutPass.process(partialTick);
            blitPass.process(partialTick);
            mainTarget.bindWrite(false);
        } catch (IOException | RuntimeException exception) {
            LOGGER.error("Could not apply the Battlezone outside-zone shader", exception);
            releasePasses();
        }
    }

    private static void ensurePasses(Minecraft minecraft) throws IOException {
        RenderTarget mainTarget = minecraft.getMainRenderTarget();
        if (whiteoutPass != null && targetWidth == mainTarget.width && targetHeight == mainTarget.height) {
            return;
        }

        releasePasses();
        targetWidth = mainTarget.width;
        targetHeight = mainTarget.height;
        scratchTarget = new TextureTarget(targetWidth, targetHeight, true, Minecraft.ON_OSX);
        scratchTarget.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);

        ResourceManager resources = minecraft.getResourceManager();
        whiteoutPass = new PostPass(resources, ResourceLocation.fromNamespaceAndPath("blockzone", "battlezone_whiteout").toString(),
                mainTarget, scratchTarget);
        whiteoutPass.addAuxAsset("DepthSampler", mainTarget::getDepthTextureId, targetWidth, targetHeight);
        blitPass = new PostPass(resources, "blit", scratchTarget, mainTarget);
        Matrix4f ortho = new Matrix4f().setOrtho(0.0F, targetWidth, 0.0F, targetHeight, 0.1F, 1000.0F);
        whiteoutPass.setOrthoMatrix(ortho);
        blitPass.setOrthoMatrix(ortho);
    }

    private static void renderCircleWall(BattlezoneClientState.Snapshot state, PoseStack poseStack) {
        float radius = state.radius();
        if (radius <= 0.0F) {
            return;
        }
        double minY = Math.min(state.y1(), state.y2());
        double maxY = Math.max(state.y1(), state.y2()) + 1.0;
        double centerX = state.centerX();
        double centerZ = state.centerZ();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        Matrix4f pose = poseStack.last().pose();
        for (int segment = 0; segment < CIRCLE_SEGMENTS; segment++) {
            double angle0 = segment * (Math.PI * 2.0 / CIRCLE_SEGMENTS);
            double angle1 = (segment + 1) * (Math.PI * 2.0 / CIRCLE_SEGMENTS);
            float x0 = (float) (centerX + Math.cos(angle0) * radius);
            float z0 = (float) (centerZ + Math.sin(angle0) * radius);
            float x1 = (float) (centerX + Math.cos(angle1) * radius);
            float z1 = (float) (centerZ + Math.sin(angle1) * radius);
            addVertex(builder, pose, x0, (float) minY, z0, 58);
            addVertex(builder, pose, x1, (float) minY, z1, 58);
            addVertex(builder, pose, x1, (float) maxY, z1, 18);
            addVertex(builder, pose, x0, (float) maxY, z0, 18);
        }
        BufferUploader.drawWithShader(builder.end());

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private static void addVertex(VertexConsumer consumer, Matrix4f pose, float x, float y, float z, int alpha) {
        consumer.vertex(pose, x, y, z).color(65, 210, 255, alpha).endVertex();
    }

    private static void releasePasses() {
        if (whiteoutPass != null) {
            whiteoutPass.close();
            whiteoutPass = null;
        }
        if (blitPass != null) {
            blitPass.close();
            blitPass = null;
        }
        if (scratchTarget != null) {
            scratchTarget.destroyBuffers();
            scratchTarget = null;
        }
        targetWidth = -1;
        targetHeight = -1;
    }

    private static final class AreaRenderer {
        private static void renderMapBounds(BattlezoneClientState.Snapshot state, PoseStack poseStack,
                                            MultiBufferSource buffers) {
            com.ptcrys.fpsmatch.core.data.AreaData area = new com.ptcrys.fpsmatch.core.data.AreaData(
                    new BlockPos(state.x1(), state.y1(), state.z1()),
                    new BlockPos(state.x2(), state.y2(), state.z2()));
            area.renderArea(poseStack, buffers, 0xFF42D9FF);
        }
    }
}
