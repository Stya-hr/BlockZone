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
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

@Mod.EventBusSubscriber(modid = "blockzone", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class BattlezoneClientRendering {
    private static final Logger LOGGER = LoggerFactory.getLogger(BattlezoneClientRendering.class);
    private static final float WARNING_FENCE_TEXTURE_BLOCKS = 3.0F;
    private static final int DOME_LONGITUDE_STEPS = 96;
    private static final int DOME_LATITUDE_STEPS = 32;
    private static final ResourceLocation DOME_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/white_concrete.png");
    private static final ResourceLocation WARNING_FENCE_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("blockzone", "textures/effect/battlezone_warning_fence.png");

    private static PostPass whiteoutPass;
    private static PostPass blitPass;
    private static TextureTarget scratchTarget;
    private static int targetWidth = -1;
    private static int targetHeight = -1;
    private static Matrix4f inverseProjection;
    private static Matrix4f worldFromView;
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
            if (state.whiteoutActive()) {
                renderDome(state, event.getPoseStack(), event.getCamera().getPosition());
            }
            return;
        }
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            return;
        }

        cameraPosition = event.getCamera().getPosition();
        inverseProjection = new Matrix4f(event.getProjectionMatrix()).invert();
        // Capture the current view-to-world rotation for the dome's world-anchored field.
        worldFromView = new Matrix4f().rotation(event.getCamera().rotation());
        renderWhiteout(event.getPartialTick());
    }

    private static void renderWhiteout(float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        BattlezoneClientState.Snapshot state = BattlezoneClientState.current(partialTick);
        if (state == null || !state.whiteoutActive() || minecraft.level == null
                || !minecraft.level.dimension().location().equals(state.dimension())
                || inverseProjection == null || worldFromView == null || cameraPosition == null) {
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
            whiteoutPass.getEffect().safeGetUniform("WorldFromView").set(worldFromView);
            Vector3f domeCenterFromCamera = new Vector3f(
                    (float) (state.centerX() - cameraX),
                    (float) (Math.min(state.y1(), state.y2()) - cameraY),
                    (float) (state.centerZ() - cameraZ));
            new Matrix4f(worldFromView).invert().transformDirection(domeCenterFromCamera);
            whiteoutPass.getEffect().safeGetUniform("DomeCenter").set(
                    domeCenterFromCamera.x, domeCenterFromCamera.y, domeCenterFromCamera.z);
            whiteoutPass.getEffect().safeGetUniform("DomeRadius").set(state.radius());
            whiteoutPass.getEffect().safeGetUniform("DomeHeight").set(state.radius());
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

    private static void renderDome(BattlezoneClientState.Snapshot state, PoseStack poseStack, Vec3 camera) {
        float radius = state.radius();
        if (radius <= 0.0F) {
            return;
        }

        double centerX = state.centerX();
        double centerY = Math.min(state.y1(), state.y2());
        double centerZ = state.centerZ();
        poseStack.pushPose();
        poseStack.translate(-camera.x, -camera.y, -camera.z);
        try {
            MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();
            RenderType renderType = RenderType.entityTranslucent(DOME_TEXTURE);
            VertexConsumer consumer = buffers.getBuffer(renderType);
            PoseStack.Pose pose = poseStack.last();

            // Build an upper hemisphere directly around the server-synchronized circle.
            // Vertices and procedural colors stay in zone coordinates as the camera moves.
            for (int latitude = 0; latitude < DOME_LATITUDE_STEPS; latitude++) {
                double theta0 = (Math.PI * 0.5) * latitude / DOME_LATITUDE_STEPS;
                double theta1 = (Math.PI * 0.5) * (latitude + 1) / DOME_LATITUDE_STEPS;
                for (int longitude = 0; longitude < DOME_LONGITUDE_STEPS; longitude++) {
                    double phi0 = (Math.PI * 2.0) * longitude / DOME_LONGITUDE_STEPS;
                    double phi1 = (Math.PI * 2.0) * (longitude + 1) / DOME_LONGITUDE_STEPS;
                    addDomeVertex(consumer, pose, centerX, centerY, centerZ, radius, theta0, phi0);
                    addDomeVertex(consumer, pose, centerX, centerY, centerZ, radius, theta0, phi1);
                    addDomeVertex(consumer, pose, centerX, centerY, centerZ, radius, theta1, phi1);
                    addDomeVertex(consumer, pose, centerX, centerY, centerZ, radius, theta1, phi0);
                }
            }
            buffers.endBatch(renderType);
        } finally {
            poseStack.popPose();
        }
    }

    private static void addDomeVertex(VertexConsumer consumer, PoseStack.Pose pose,
                                      double centerX, double centerY, double centerZ, float radius,
                                      double theta, double phi) {
        float nx = (float) (Math.sin(theta) * Math.cos(phi));
        float ny = (float) Math.cos(theta);
        float nz = (float) (Math.sin(theta) * Math.sin(phi));
        int color = domeColor(nx, ny, nz);
        consumer.vertex(pose.pose(), (float) (centerX + nx * radius), (float) (centerY + ny * radius),
                        (float) (centerZ + nz * radius))
                .color((color >>> 24) & 0xFF, (color >>> 16) & 0xFF,
                        (color >>> 8) & 0xFF, color & 0xFF)
                .uv((float) (phi / (Math.PI * 2.0)), (float) (theta / (Math.PI * 0.5)))
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT)
                .normal(pose.normal(), nx, ny, nz)
                .endVertex();
    }

    private static int domeColor(float x, float y, float z) {
        // Static coordinates give the field a stable world-space appearance.
        double azimuth = Math.atan2(z, x) + y * 2.6;
        double orbit = Math.sin(azimuth * 5.0 + (1.0 - y) * 17.0 + Math.sin(azimuth * 2.0) * 1.4);
        double crossFlow = Math.sin(x * 9.0 - z * 6.0 + Math.sin(y * 10.0) * 1.2);
        double flow = 0.5 + 0.5 * (orbit * 0.68 + crossFlow * 0.32);
        double filament = smoothstep(0.48, 0.88, flow);
        double red = lerp(lerp(0.34, 0.48, flow * 0.60), lerp(0.22, 0.92, filament), filament * 0.66);
        double green = lerp(lerp(0.40, 0.56, flow * 0.60), lerp(0.82, 0.48, filament), filament * 0.66);
        double blue = lerp(lerp(0.58, 0.70, flow * 0.60), lerp(0.96, 0.82, filament), filament * 0.66);
        double rim = Math.pow(1.0 - Math.abs(y), 2.5);
        red = Math.min(1.0, red + rim * 0.34);
        green = Math.min(1.0, green + rim * 0.42);
        blue = Math.min(1.0, blue + rim * 0.56);
        int alpha = (int) (255.0 * Math.min(0.4, 0.09 + flow * 0.07 + filament * 0.08 + rim * 0.18));
        int redByte = (int) (red * 255.0);
        int greenByte = (int) (green * 255.0);
        int blueByte = (int) (blue * 255.0);
        return (redByte << 24) | (greenByte << 16) | (blueByte << 8) | alpha;
    }

    private static double smoothstep(double edge0, double edge1, double value) {
        double t = Math.max(0.0, Math.min(1.0, (value - edge0) / (edge1 - edge0)));
        return t * t * (3.0 - 2.0 * t);
    }

    private static double lerp(double start, double end, double amount) {
        return start + (end - start) * amount;
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
