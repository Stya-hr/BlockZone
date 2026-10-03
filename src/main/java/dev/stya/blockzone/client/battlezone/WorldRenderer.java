package dev.stya.blockzone.client.battlezone;

import dev.stya.blockzone.net.battlezone.BoundaryPreviewS2CPacket;
import dev.stya.blockzone.util.battlezone.BoundaryGeometry;
import dev.stya.blockzone.util.battlezone.WarningRibbonGeometry;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;

@Mod.EventBusSubscriber(modid = "blockzone", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class WorldRenderer {
    private static final Logger LOGGER = LoggerFactory.getLogger(WorldRenderer.class);
    private static final ResourceLocation WARNING_FENCE_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("blockzone", "textures/effect/battlezone_warning_fence.png");

    private static ResourceLocation cachedFenceTexture;
    private static ResourceLocation resolvedFenceTexture;
    private static float cachedFenceAspect = 1.0F;

    private WorldRenderer() {
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
                SphereRenderer.release();
                cachedFenceTexture = null;
            }
        });
    }

    static void renderWorld(RenderLevelStageEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        ZoneClientState.Snapshot state = ZoneClientState.current(event.getPartialTick());
        if (minecraft.level == null) {
            return;
        }

        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_SOLID_BLOCKS) {
            var zonePreview = ZoneClientState.zonePreview();
            if (zonePreview != null && minecraft.level.dimension().location().equals(zonePreview.dimension())) {
                renderZoneGrid(zonePreview.zone(), event.getPoseStack(), event.getCamera().getPosition());
            }
            BoundaryPreviewS2CPacket preview = ZoneClientState.preview();
            if (preview != null && minecraft.level.dimension().location().equals(preview.dimension())) {
                renderPreview(preview, event.getPoseStack(), event.getCamera().getPosition());
            }
            return;
        }
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_SKY) {
            MaterialRenderState.begin(event, state);
        }
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            MaterialRenderState.end();
        }
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_PARTICLES && state != null
                && minecraft.level.dimension().location().equals(state.dimension())) {
            if (state.whiteoutActive()) {
                SphereRenderer.render(event, state);
            }
            if (minecraft.player != null) {
                renderWarningFence(state, event.getPoseStack(), event.getCamera().getPosition(),
                        minecraft.player, event.getPartialTick());
            }
        }
    }

    private static void renderWarningFence(ZoneClientState.Snapshot state, PoseStack poseStack,
                                           Vec3 camera, LocalPlayer player, float partialTick) {
        if (player.isSpectator()) {
            return;
        }
        BoundaryGeometry bounds = BoundaryGeometry.of(
                state.x1(), state.z1(), state.x2(), state.z2());
        double minX = bounds.minX();
        double maxX = bounds.maxX();
        double minZ = bounds.minZ();
        double maxZ = bounds.maxZ();
        var box = player.getBoundingBox();
        boolean overlapsZ = box.maxZ >= minZ && box.minZ <= maxZ;
        boolean overlapsX = box.maxX >= minX && box.minX <= maxX;
        float west = overlapsZ ? WarningRibbonGeometry.proximityAlpha(minX, box.minX, box.maxX) : 0;
        float east = overlapsZ ? WarningRibbonGeometry.proximityAlpha(maxX, box.minX, box.maxX) : 0;
        float north = overlapsX ? WarningRibbonGeometry.proximityAlpha(minZ, box.minZ, box.maxZ) : 0;
        float south = overlapsX ? WarningRibbonGeometry.proximityAlpha(maxZ, box.minZ, box.maxZ) : 0;
        if (west + east + north + south <= 0) return;

        Vec3 position = player.getPosition(partialTick);
        double playerX = Math.max(minX, Math.min(maxX, position.x));
        double playerZ = Math.max(minZ, Math.min(maxZ, position.z));
        double bottom = position.y + 0.9;
        ResourceLocation texture = fenceTexture(state.boundaryTexture());
        MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        RenderType renderType = ZoneRenderTypes.boundary(texture);
        VertexConsumer consumer = buffers.getBuffer(renderType);
        renderWarningSide(consumer, poseStack.last(), camera, true, minX + 0.002,
                minZ, maxZ, playerZ, bottom, west, true);
        renderWarningSide(consumer, poseStack.last(), camera, true, maxX - 0.002,
                minZ, maxZ, playerZ, bottom, east, false);
        renderWarningSide(consumer, poseStack.last(), camera, false, minZ + 0.002,
                minX, maxX, playerX, bottom, north, false);
        renderWarningSide(consumer, poseStack.last(), camera, false, maxZ - 0.002,
                minX, maxX, playerX, bottom, south, true);
        buffers.endBatch(renderType);
    }

    private static void renderWarningSide(VertexConsumer consumer, PoseStack.Pose pose, Vec3 camera,
                                          boolean xPlane, double plane, double min, double max,
                                          double center, double bottom, float proximity, boolean reverse) {
        if (proximity <= 0) return;
        double start = Math.max(min, center - WarningRibbonGeometry.HALF_LENGTH);
        double end = Math.min(max, center + WarningRibbonGeometry.HALF_LENGTH);
        double tileWidth = WarningRibbonGeometry.HEIGHT * cachedFenceAspect;
        double top = bottom + WarningRibbonGeometry.HEIGHT;
        for (double a = start; a < end;) {
            double b = Math.min(end, a + 0.5);
            double sign = reverse ? -1 : 1;
            addTexturedFenceVertex(consumer, pose, camera, xPlane, plane, a, bottom,
                    sign * a / tileWidth, 1, center, proximity);
            addTexturedFenceVertex(consumer, pose, camera, xPlane, plane, b, bottom,
                    sign * b / tileWidth, 1, center, proximity);
            addTexturedFenceVertex(consumer, pose, camera, xPlane, plane, b, top,
                    sign * b / tileWidth, 0, center, proximity);
            addTexturedFenceVertex(consumer, pose, camera, xPlane, plane, a, top,
                    sign * a / tileWidth, 0, center, proximity);
            a = b;
        }
    }

    private static void addTexturedFenceVertex(VertexConsumer consumer, PoseStack.Pose pose, Vec3 camera,
                                               boolean xPlane, double plane, double along, double y,
                                               double u, double v, double center, float proximity) {
        int alpha = Math.round(255 * proximity * WarningRibbonGeometry.endAlpha(along, center));
        consumer.vertex(pose.pose(), (float)((xPlane ? plane : along) - camera.x),
                        (float)(y - camera.y), (float)((xPlane ? along : plane) - camera.z))
                .color(255, 255, 255, alpha)
                .uv((float)u, (float)v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT)
                .normal(pose.normal(), xPlane ? 1.0F : 0.0F, 0.0F, xPlane ? 0.0F : 1.0F)
                .endVertex();
    }

    private static ResourceLocation fenceTexture(String configuredTexture) {
        ResourceLocation requested = ResourceLocation.tryParse(configuredTexture);
        if (requested == null) {
            requested = WARNING_FENCE_TEXTURE;
        }
        if (requested.equals(cachedFenceTexture)) {
            return resolvedFenceTexture;
        }
        cachedFenceTexture = requested;
        resolvedFenceTexture = requested;
        cachedFenceAspect = readFenceAspect(requested);
        if (cachedFenceAspect <= 0.0F) {
            LOGGER.warn("Invalid or missing Battlezone boundary texture {}; using {}", requested, WARNING_FENCE_TEXTURE);
            resolvedFenceTexture = WARNING_FENCE_TEXTURE;
            cachedFenceAspect = readFenceAspect(WARNING_FENCE_TEXTURE);
            if (cachedFenceAspect <= 0.0F) {
                cachedFenceAspect = 1.0F;
            }
        }
        return resolvedFenceTexture;
    }

    private static float readFenceAspect(ResourceLocation texture) {
        var resource = Minecraft.getInstance().getResourceManager().getResource(texture);
        if (resource.isEmpty()) {
            return 0.0F;
        }
        try (var input = resource.get().open(); NativeImage image = NativeImage.read(input)) {
            return (float) image.getWidth() / image.getHeight();
        } catch (IOException | RuntimeException exception) {
            LOGGER.warn("Could not read Battlezone boundary texture {}", texture, exception);
            return 0.0F;
        }
    }

    private static void renderZoneGrid(dev.stya.blockzone.util.battlezone.ZoneGeometry zone, PoseStack stack, Vec3 camera) {
        var buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        var type = RenderType.lines();
        var consumer = buffers.getBuffer(type);
        for (int latitude = -7; latitude <= 7; latitude++) {
            double phi = latitude * Math.PI / 16;
            for (int segment = 0; segment < 128; segment++) {
                gridLine(consumer, stack.last(), camera, zone,
                        gridPoint(phi, segment * Math.PI / 64), gridPoint(phi, (segment + 1) * Math.PI / 64));
            }
        }
        for (int longitude = 0; longitude < 32; longitude++) {
            double theta = longitude * Math.PI / 16;
            for (int segment = 0; segment < 64; segment++) {
                gridLine(consumer, stack.last(), camera, zone,
                        gridPoint(-Math.PI / 2 + segment * Math.PI / 64, theta),
                        gridPoint(-Math.PI / 2 + (segment + 1) * Math.PI / 64, theta));
            }
        }
        if (zone.radius() == 0) {
            gridLine(consumer, stack.last(), camera, new dev.stya.blockzone.util.battlezone.ZoneGeometry(
                    zone.centerX(), zone.centerY(), zone.centerZ(), 1), new Vec3(-1, 0, 0), new Vec3(1, 0, 0));
        }
        buffers.endBatch(type);
    }

    private static Vec3 gridPoint(double phi, double theta) {
        return new Vec3(Math.cos(phi) * Math.cos(theta), Math.sin(phi), Math.cos(phi) * Math.sin(theta));
    }

    private static void gridLine(VertexConsumer consumer, PoseStack.Pose pose, Vec3 camera,
                                 dev.stya.blockzone.util.battlezone.ZoneGeometry zone, Vec3 a, Vec3 b) {
        Vec3 direction = b.subtract(a).normalize();
        for (Vec3 point : new Vec3[]{a, b}) {
            consumer.vertex(pose.pose(), (float)(zone.centerX() + point.x * zone.radius() - camera.x),
                            (float)(zone.centerY() + point.y * zone.radius() - camera.y),
                            (float)(zone.centerZ() + point.z * zone.radius() - camera.z))
                    .color(100, 225, 255, 230)
                    .normal(pose.normal(), (float)direction.x, (float)direction.y, (float)direction.z).endVertex();
        }
    }

    private static void renderPreview(BoundaryPreviewS2CPacket preview, PoseStack stack, Vec3 camera) {
        double minX = Math.min(preview.pos1().getX(), preview.pos2().getX());
        double maxX = Math.max(preview.pos1().getX(), preview.pos2().getX()) + 1.0;
        double minY = Math.min(preview.pos1().getY(), preview.pos2().getY());
        double maxY = Math.max(preview.pos1().getY(), preview.pos2().getY()) + 1.0;
        double minZ = Math.min(preview.pos1().getZ(), preview.pos2().getZ());
        double maxZ = Math.max(preview.pos1().getZ(), preview.pos2().getZ()) + 1.0;
        stack.pushPose();
        try {
            MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();
            RenderType type = RenderType.lines();
            VertexConsumer consumer = buffers.getBuffer(type);
            previewFace(consumer, stack.last(), camera, true, minX, minZ, maxZ, minY, maxY);
            previewFace(consumer, stack.last(), camera, true, maxX, minZ, maxZ, minY, maxY);
            previewFace(consumer, stack.last(), camera, false, minZ, minX, maxX, minY, maxY);
            previewFace(consumer, stack.last(), camera, false, maxZ, minX, maxX, minY, maxY);
            buffers.endBatch(type);
        } finally {
            stack.popPose();
        }
    }

    private static void previewFace(VertexConsumer consumer, PoseStack.Pose pose, Vec3 camera,
                                    boolean xPlane, double plane, double min, double max,
                                    double minY, double maxY) {
        double cameraAlong = xPlane ? camera.z : camera.x;
        double cameraPlane = xPlane ? camera.x : camera.z;
        if (Math.abs(cameraPlane - plane) > 96.0 || cameraAlong < min - 96.0 || cameraAlong > max + 96.0) {
            return;
        }
        double start = Math.max(min, cameraAlong - 96.0);
        double end = Math.min(max, cameraAlong + 96.0);
        double lowY = Math.max(minY, camera.y - 96.0);
        double highY = Math.min(maxY, camera.y + 96.0);
        if (highY < lowY) {
            return;
        }
        for (double along = Math.ceil(start / 2.0) * 2.0; along <= end; along += 2.0) {
            previewLine(consumer, pose, camera, xPlane, plane, along, lowY, along, highY);
        }
        previewLine(consumer, pose, camera, xPlane, plane, min, minY, min, maxY);
        previewLine(consumer, pose, camera, xPlane, plane, max, minY, max, maxY);
        for (double y = Math.ceil(lowY / 2.0) * 2.0; y <= highY; y += 2.0) {
            previewLine(consumer, pose, camera, xPlane, plane, start, y, end, y);
        }
        previewLine(consumer, pose, camera, xPlane, plane, start, minY, end, minY);
        previewLine(consumer, pose, camera, xPlane, plane, start, maxY, end, maxY);
    }

    private static void previewLine(VertexConsumer consumer, PoseStack.Pose pose, Vec3 camera,
                                    boolean xPlane, double plane, double a, double ay, double b, double by) {
        double dx = xPlane ? 0.0 : b - a;
        double dy = by - ay;
        double dz = xPlane ? b - a : 0.0;
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (length < 1.0e-6) {
            return;
        }
        previewVertex(consumer, pose, camera, xPlane, plane, a, ay,
                (float) (dx / length), (float) (dy / length), (float) (dz / length));
        previewVertex(consumer, pose, camera, xPlane, plane, b, by,
                (float) (dx / length), (float) (dy / length), (float) (dz / length));
    }

    private static void previewVertex(VertexConsumer consumer, PoseStack.Pose pose, Vec3 camera,
                                      boolean xPlane, double plane, double along, double y,
                                      float dx, float dy, float dz) {
        float x = (float) ((xPlane ? plane : along) - camera.x);
        float z = (float) ((xPlane ? along : plane) - camera.z);
        consumer.vertex(pose.pose(), x, (float) (y - camera.y), z)
                .color(100, 225, 255, 210)
                .normal(pose.normal(), dx, dy, dz)
                .endVertex();
    }

}
