package dev.stya.blockzone.game.battlezone;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
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
    private static final float WARNING_FENCE_TEXTURE_BLOCKS = 3.0F;
    private static final ResourceLocation WARNING_FENCE_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("blockzone", "textures/effect/battlezone_warning_fence.png");

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
        Minecraft minecraft = Minecraft.getInstance();
        BattlezoneClientState.Snapshot state = BattlezoneClientState.current(event.getPartialTick());
        if (minecraft.level == null || state == null
                || !minecraft.level.dimension().location().equals(state.dimension())) {
            return;
        }

        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_SOLID_BLOCKS) {
            LocalPlayer player = minecraft.player;
            if (player != null) {
                renderWarningFence(state, event.getPoseStack(), event.getCamera().getPosition(), player);
            }
            return;
        }
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            return;
        }

        cameraPosition = event.getCamera().getPosition();
        inverseProjection = new Matrix4f(event.getProjectionMatrix()).invert();
        // Pair the world-space ray basis with the camera position captured above.
        // This keeps dome intersection and material coordinates in one camera snapshot.
        inverseViewRotation = new Matrix4f().rotation(event.getCamera().rotation());
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
            double cameraY = cameraPosition.y;
            double cameraZ = cameraPosition.z;
            whiteoutPass.getEffect().safeGetUniform("InverseProjection").set(inverseProjection);
            whiteoutPass.getEffect().safeGetUniform("InverseViewRotation").set(inverseViewRotation);
            whiteoutPass.getEffect().safeGetUniform("DomeCenter").set(
                    (float) (state.centerX() - cameraX),
                    (float) (Math.min(state.y1(), state.y2()) - cameraY),
                    (float) (state.centerZ() - cameraZ));
            whiteoutPass.getEffect().safeGetUniform("DomeRadius").set(state.radius());
            whiteoutPass.getEffect().safeGetUniform("DomeHeight").set(state.radius());
            whiteoutPass.getEffect().safeGetUniform("GameTime").set(
                    (minecraft.level.getGameTime() + partialTick) / 20.0F);

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

    private static void renderWarningFence(BattlezoneClientState.Snapshot state, PoseStack poseStack,
                                           Vec3 camera, LocalPlayer player) {
        double minX = Math.min(state.x1(), state.x2());
        double maxX = Math.max(state.x1(), state.x2()) + 1.0;
        double minZ = Math.min(state.z1(), state.z2());
        double maxZ = Math.max(state.z1(), state.z2()) + 1.0;

        // Keep the full perimeter visible at the local player's height.
        double fenceMinY = player.getY();
        double fenceMaxY = fenceMinY + player.getBbHeight();

        poseStack.pushPose();
        poseStack.translate(-camera.x, -camera.y, -camera.z);
        try {
            MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();
            RenderType renderType = RenderType.entityTranslucent(WARNING_FENCE_TEXTURE);
            VertexConsumer consumer = buffers.getBuffer(renderType);
            renderFenceSide(consumer, poseStack.last(), true, minX, minZ, maxZ, fenceMinY, fenceMaxY);
            renderFenceSide(consumer, poseStack.last(), true, maxX, minZ, maxZ, fenceMinY, fenceMaxY);
            renderFenceSide(consumer, poseStack.last(), false, minZ, minX, maxX, fenceMinY, fenceMaxY);
            renderFenceSide(consumer, poseStack.last(), false, maxZ, minX, maxX, fenceMinY, fenceMaxY);
            buffers.endBatch(renderType);
        } finally {
            poseStack.popPose();
        }
    }

    private static void renderFenceSide(VertexConsumer consumer, PoseStack.Pose pose, boolean xPlane, double plane,
                                        double alongMin, double alongMax, double minY, double maxY) {
        // Keep every side in fixed map coordinates so the fence stays anchored to the zone.
        double uMin = alongMin / WARNING_FENCE_TEXTURE_BLOCKS;
        double uMax = alongMax / WARNING_FENCE_TEXTURE_BLOCKS;
        double vMin = minY / WARNING_FENCE_TEXTURE_BLOCKS;
        double vMax = maxY / WARNING_FENCE_TEXTURE_BLOCKS;
        addTexturedFenceVertex(consumer, pose, xPlane, plane, alongMin, minY, uMin, vMin);
        addTexturedFenceVertex(consumer, pose, xPlane, plane, alongMax, minY, uMax, vMin);
        addTexturedFenceVertex(consumer, pose, xPlane, plane, alongMax, maxY, uMax, vMax);
        addTexturedFenceVertex(consumer, pose, xPlane, plane, alongMin, maxY, uMin, vMax);
    }

    private static void addTexturedFenceVertex(VertexConsumer consumer, PoseStack.Pose pose, boolean xPlane,
                                               double plane, double along, double y,
                                               double u, double v) {
        float normalX = xPlane ? 1.0F : 0.0F;
        float normalZ = xPlane ? 0.0F : 1.0F;
        consumer.vertex(pose.pose(), (float) (xPlane ? plane : along), (float) y,
                        (float) (xPlane ? along : plane))
                .color(255, 255, 255, 255)
                .uv((float) u, (float) v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT)
                .normal(pose.normal(), normalX, 0.0F, normalZ)
                .endVertex();
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

}
