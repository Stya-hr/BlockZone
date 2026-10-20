package dev.stya.blockzone.client.loot;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import dev.stya.blockzone.BlockZone;
import dev.stya.blockzone.loot.LootDropEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.opengl.GL11;
import java.util.HashMap;
import java.util.Map;

/** A separate one-pixel silhouette, tested against scene depth instead of vanilla's through-wall glow. */
@Mod.EventBusSubscriber(modid = BlockZone.MOD_ID, value = Dist.CLIENT)
public final class DropOutlines {
    private static PostChain effect;
    private static boolean failed;
    private static org.joml.Matrix4f worldView;
    private static org.joml.Matrix4f worldProjection;
    private static final MultiBufferSource.BufferSource BUFFERS = MultiBufferSource.immediate(new BufferBuilder(256));
    private static final VertexConsumer DISCARD = new WhiteVertices(null);
    private static final Map<RenderType, RenderType> TYPES = new HashMap<>();
    private DropOutlines() { }

    public static void reload() {
        if (effect != null) effect.close();
        effect = null;
        failed = false;
        TYPES.clear();
        worldView = null;
        worldProjection = null;
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        // Forge's AFTER_LEVEL event carries the projection stack, not the world view stack.
        // Capture the same camera transform used by entities, including camera effects.
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            worldView = new org.joml.Matrix4f(event.getPoseStack().last().pose());
            worldProjection = new org.joml.Matrix4f(event.getProjectionMatrix());
            return;
        }
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL || worldView == null) return;
        var mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || failed || mc.options.hideGui) return;
        var drops = mc.level.getEntitiesOfClass(LootDropEntity.class, mc.player.getBoundingBox().inflate(6),
                drop -> drop.isAlive() && mc.player.distanceToSqr(drop) <= 36);
        if (drops.isEmpty()) return;
        var main = mc.getMainRenderTarget();
        if (effect == null) {
            try {
                effect = new PostChain(mc.getTextureManager(), mc.getResourceManager(), main,
                        ResourceLocation.fromNamespaceAndPath(BlockZone.MOD_ID, "shaders/post/drop_outline.json"));
                effect.resize(main.width, main.height);
            } catch (Exception failure) {
                failed = true;
                if (effect != null) effect.close();
                effect = null;
                com.mojang.logging.LogUtils.getLogger().error("Cannot load ground item outline", failure);
                return;
            }
        }
        var mask = effect.getTempTarget("mask");
        if (mask.width != main.width || mask.height != main.height) effect.resize(main.width, main.height);
        var projection = new org.joml.Matrix4f(RenderSystem.getProjectionMatrix());
        var sorting = RenderSystem.getVertexSorting();
        var pose = new PoseStack();
        pose.mulPoseMatrix(worldView);
        try {
            RenderSystem.setProjectionMatrix(event.getProjectionMatrix(), VertexSorting.DISTANCE_TO_ORIGIN);
            mask.clear(Minecraft.ON_OSX);
            mask.copyDepthFrom(main);
            main.bindWrite(false);
            MultiBufferSource outlines = type -> {
                if (type.outline().isEmpty()) return DISCARD; // Glint has no silhouette.
                var source = type.outline().get();
                var depthType = TYPES.computeIfAbsent(source, original -> new RenderType("blockzone_drop_outline",
                        DefaultVertexFormat.POSITION_COLOR_TEX, VertexFormat.Mode.QUADS, 256, false, false,
                        () -> {
                            original.setupRenderState();
                            mask.bindWrite(false);
                            RenderSystem.enableDepthTest();
                            RenderSystem.depthFunc(GL11.GL_LEQUAL);
                            RenderSystem.depthMask(false);
                        }, () -> { original.clearRenderState(); main.bindWrite(false); }) { });
                return new WhiteVertices(BUFFERS.getBuffer(depthType));
            };
            var camera = event.getCamera().getPosition();
            float partial = event.getPartialTick();
            for (var drop : drops) {
                if (!event.getFrustum().isVisible(drop.getBoundingBox().inflate(1))) continue;
                pose.pushPose();
                try {
                    pose.translate(Mth.lerp(partial, drop.xOld, drop.getX()) - camera.x,
                            Mth.lerp(partial, drop.yOld, drop.getY()) - camera.y,
                            Mth.lerp(partial, drop.zOld, drop.getZ()) - camera.z);
                    if (mc.getEntityRenderDispatcher().getRenderer(drop) instanceof LootDropRenderer renderer)
                        renderer.renderItem(drop, partial, pose, outlines, mc.getEntityRenderDispatcher().getPackedLightCoords(drop, partial));
                } finally { pose.popPose(); }
            }
            BUFFERS.endBatch();
            effect.process(partial);
            main.bindWrite(false);
            RenderSystem.enableBlend();
            RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                    GlStateManager.SourceFactor.ZERO, GlStateManager.DestFactor.ONE);
            effect.getTempTarget("edge").blitToScreen(main.width, main.height, false);
        } finally {
            main.bindWrite(false);
            RenderSystem.disableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
            RenderSystem.setProjectionMatrix(projection, sorting);
        }
    }

    /** Project the item's spatial anchor using the actual rendered camera, including FOV and view bob. */
    public static org.joml.Vector2f project(LootDropEntity drop, float partial, float width, float height) {
        if (worldView == null || worldProjection == null) return null;
        var camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        var clip = new org.joml.Vector4f(
                (float)(Mth.lerp(partial, drop.xOld, drop.getX()) - camera.x),
                (float)(Mth.lerp(partial, drop.yOld, drop.getY()) + drop.getBbHeight() / 2 - camera.y),
                (float)(Mth.lerp(partial, drop.zOld, drop.getZ()) - camera.z), 1)
                .mul(worldView).mul(worldProjection);
        if (clip.w <= 0 || Math.abs(clip.z) > clip.w) return null;
        return new org.joml.Vector2f((clip.x / clip.w + 1) * width / 2, (1 - clip.y / clip.w) * height / 2);
    }

    /** Retain texture cutouts while discarding item tint, normals and lightmap attributes. */
    private record WhiteVertices(VertexConsumer delegate) implements VertexConsumer {
        @Override public VertexConsumer vertex(double x, double y, double z) { if (delegate != null) delegate.vertex(x, y, z); return this; }
        @Override public VertexConsumer color(int r, int g, int b, int a) { return this; }
        @Override public VertexConsumer uv(float u, float v) { if (delegate != null) delegate.color(255, 255, 255, 255).uv(u, v); return this; }
        @Override public VertexConsumer overlayCoords(int u, int v) { return this; }
        @Override public VertexConsumer uv2(int u, int v) { return this; }
        @Override public VertexConsumer normal(float x, float y, float z) { return this; }
        @Override public void endVertex() { if (delegate != null) delegate.endVertex(); }
        @Override public void defaultColor(int r, int g, int b, int a) { }
        @Override public void unsetDefaultColor() { }
    }

    @Mod.EventBusSubscriber(modid = BlockZone.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Reload {
        @SubscribeEvent public static void register(RegisterClientReloadListenersEvent event) {
            event.registerReloadListener((net.minecraft.server.packs.resources.ResourceManagerReloadListener) manager -> reload());
        }
    }
}
