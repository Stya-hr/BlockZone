package dev.stya.blockzone.client.compat;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import dev.stya.blockzone.client.battlezone.ZoneClientState;
import dev.stya.blockzone.zone.BoundaryGeometry;
import dev.stya.blockzone.zone.ZoneShape;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.io.IOException;

/** Styles only Xaero's final terrain quad; player markers retain their own crisp rendering. */
@Mod.EventBusSubscriber(modid = "blockzone", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class TacticalMinimap {
    private static ShaderInstance shader;
    private TacticalMinimap() {}
    @SubscribeEvent public static void register(RegisterShadersEvent event) throws IOException {
        event.registerShader(new ShaderInstance(event.getResourceProvider(),
                ResourceLocation.fromNamespaceAndPath("blockzone", "tactical_minimap"), DefaultVertexFormat.POSITION_TEX),
                value -> shader = value);
    }
    public static boolean draw(PoseStack pose, float x, float y, int u, int v, float width,
                               float height, float sampleHeight, float textureSize) {
        var mc = Minecraft.getInstance();
        var state = ZoneClientState.minimap(mc.getFrameTime());
        if (shader == null || state == null || mc.getCameraEntity() == null) return false;
        var camera = mc.getCameraEntity().getPosition(mc.getFrameTime());
        camera = new net.minecraft.world.phys.Vec3(XaeroMinimapCompat.viewCenter(camera.x, true), camera.y,
                XaeroMinimapCompat.viewCenter(camera.z, false));
        var bounds = BoundaryGeometry.of(state.x1(), state.z1(), state.x2(), state.z2());
        shader.safeGetUniform("UvRect").set(u / textureSize, v / textureSize, width / textureSize, sampleHeight / textureSize);
        shader.safeGetUniform("ViewSpan").set((float) XaeroMinimapCompat.viewSpan());
        // Relative coordinates retain sub-block accuracy far from the world origin.
        shader.safeGetUniform("MapBounds").set((float)(bounds.minX() - camera.x), (float)(bounds.minZ() - camera.z),
                (float)(bounds.maxX() - camera.x), (float)(bounds.maxZ() - camera.z));
        shader.safeGetUniform("SafeZone").set((float)(state.centerX() - camera.x), (float)(state.centerZ() - camera.z),
                state.radius(), state.shape() == ZoneShape.SQUARE_PRISM ? 1f : 0f);
        var previous = RenderSystem.getShader();
        try {
            RenderSystem.setShader(() -> shader);
            var matrix = pose.last().pose();
            var buffer = Tesselator.getInstance().getBuilder();
            buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
            buffer.vertex(matrix, x, y + height, 0).uv(u / textureSize, v / textureSize).endVertex();
            buffer.vertex(matrix, x + width, y + height, 0).uv((u + width) / textureSize, v / textureSize).endVertex();
            buffer.vertex(matrix, x + width, y, 0).uv((u + width) / textureSize, (v + sampleHeight) / textureSize).endVertex();
            buffer.vertex(matrix, x, y, 0).uv(u / textureSize, (v + sampleHeight) / textureSize).endVertex();
            BufferUploader.drawWithShader(buffer.end());
        } finally { RenderSystem.setShader(() -> previous); }
        return true;
    }
}
