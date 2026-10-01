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
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.PostPass;
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
    private static final double WARNING_FENCE_DISTANCE = 12.0;
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
        // AFTER_PARTICLES runs while Fabulous graphics is still rendering into its
        // separate entity/translucency targets. The main target is not the complete
        // scene there, so the post pass misses entities and block entities. AFTER_LEVEL
        // runs after those targets have been composited into the main target.
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) {
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
        // Use the inverse rotation calculated by GameRenderer for this frame. The
        // render-stage pose stack can include additional transforms and is not a
        // reliable source for reconstructing positions from the main target depth.
        inverseViewRotation = new Matrix4f().set(RenderSystem.getInverseViewRotationMatrix());

        PoseStack poseStack = event.getPoseStack();
        LocalPlayer player = minecraft.player;
        if (player != null) {
            renderWarningFence(state, cameraPosition, poseStack, player);
        }
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

    private static void renderWarningFence(BattlezoneClientState.Snapshot state, Vec3 camera, PoseStack poseStack,
                                           LocalPlayer player) {
        double minX = Math.min(state.x1(), state.x2());
        double maxX = Math.max(state.x1(), state.x2()) + 1.0;
        double minZ = Math.min(state.z1(), state.z2());
        double maxZ = Math.max(state.z1(), state.z2()) + 1.0;

        boolean west = player.getX() - minX <= WARNING_FENCE_DISTANCE;
        boolean east = maxX - player.getX() <= WARNING_FENCE_DISTANCE;
        boolean north = player.getZ() - minZ <= WARNING_FENCE_DISTANCE;
        boolean south = maxZ - player.getZ() <= WARNING_FENCE_DISTANCE;
        if (!west && !east && !north && !south) {
            return;
        }

        double areaMinY = Math.min(state.y1(), state.y2());
        double areaMaxY = Math.max(state.y1(), state.y2()) + 1.0;
        double fenceMinY = areaMinY;
        double fenceMaxY = areaMaxY;
        if (fenceMaxY <= fenceMinY) {
            return;
        }

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        Minecraft.getInstance().getTextureManager().bindForSetup(WARNING_FENCE_TEXTURE);

        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        Matrix4f pose = poseStack.last().pose();
        if (west) {
            renderFenceSide(builder, pose, camera, true, minX, minZ, maxZ, fenceMinY, fenceMaxY);
        }
        if (east) {
            renderFenceSide(builder, pose, camera, true, maxX, minZ, maxZ, fenceMinY, fenceMaxY);
        }
        if (north) {
            renderFenceSide(builder, pose, camera, false, minZ, minX, maxX, fenceMinY, fenceMaxY);
        }
        if (south) {
            renderFenceSide(builder, pose, camera, false, maxZ, minX, maxX, fenceMinY, fenceMaxY);
        }
        BufferUploader.drawWithShader(builder.end());

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private static void renderFenceSide(VertexConsumer consumer, Matrix4f pose, Vec3 camera,
                                        boolean xPlane, double plane, double alongMin, double alongMax,
                                        double minY, double maxY) {
        // Keep each side in fixed map coordinates. Only visibility depends on the player's
        // distance; the rendered segment itself must not slide along the boundary with them.
        double uMin = alongMin / WARNING_FENCE_TEXTURE_BLOCKS;
        double uMax = alongMax / WARNING_FENCE_TEXTURE_BLOCKS;
        double vMin = minY / WARNING_FENCE_TEXTURE_BLOCKS;
        double vMax = maxY / WARNING_FENCE_TEXTURE_BLOCKS;
        addTexturedFenceVertex(consumer, pose, camera, xPlane, plane, alongMin, minY, uMin, vMin);
        addTexturedFenceVertex(consumer, pose, camera, xPlane, plane, alongMax, minY, uMax, vMin);
        addTexturedFenceVertex(consumer, pose, camera, xPlane, plane, alongMax, maxY, uMax, vMax);
        addTexturedFenceVertex(consumer, pose, camera, xPlane, plane, alongMin, maxY, uMin, vMax);
    }

    private static void addTexturedFenceVertex(VertexConsumer consumer, Matrix4f pose, Vec3 camera,
                                               boolean xPlane, double plane, double along, double y,
                                               double u, double v) {
        double x = (xPlane ? plane : along) - camera.x;
        double z = (xPlane ? along : plane) - camera.z;
        consumer.vertex(pose, (float) x, (float) (y - camera.y), (float) z)
                .uv((float) u, (float) v)
                .color(255, 255, 255, 255)
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
