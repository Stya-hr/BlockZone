package dev.stya.blockzone.client.battlezone;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import dev.stya.blockzone.util.battlezone.ZoneShape;
import java.util.EnumMap;

final class ZoneRenderer {
    private static final float WALL_HEIGHT = 96f;
    private static final EnumMap<ZoneShape, VertexBuffer> MESHES = new EnumMap<>(ZoneShape.class);
    private ZoneRenderer() {}

    static void render(RenderLevelStageEvent event, ZoneClientState.Snapshot state) {
        if (state.radius() <= 0) return;
        VertexBuffer mesh = MESHES.get(state.shape());
        if (mesh == null) {
            BufferBuilder builder = new BufferBuilder(128 * 4 * 12);
            builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
            if (state.shape() == ZoneShape.SQUARE_PRISM) {
                squarePrism(builder);
            } else {
                for (int lon = 0; lon < 128; lon++) {
                    cylinderVertex(builder, -1, lon);
                    cylinderVertex(builder, 1, lon);
                    cylinderVertex(builder, 1, lon + 1);
                    cylinderVertex(builder, -1, lon + 1);
                }
            }
            mesh = new VertexBuffer(VertexBuffer.Usage.STATIC);
            mesh.bind();
            mesh.upload(builder.end());
            MESHES.put(state.shape(), mesh);
            VertexBuffer.unbind();
        }
        var camera = event.getCamera().getPosition();
        // Anchor the wall to the map floor rather than the observer's height.
        float height = WALL_HEIGHT / 2;
        double centerY = state.centerY() + height;
        Matrix4f modelView = new Matrix4f(event.getPoseStack().last().pose())
                .translate((float)(state.centerX() - camera.x), (float)(centerY - camera.y),
                        (float)(state.centerZ() - camera.z)).scale(state.radius(), height, state.radius());
        var type = ZoneRenderTypes.zone();
        type.setupRenderState();
        try {
            var shader = ZoneRenderTypes.zoneShader();
            shader.safeGetUniform("ZoneShape").set(state.shape().ordinal());
            shader.safeGetUniform("ZoneHeightRatio").set(height / state.radius());
            shader.safeGetUniform("ZoneYOffset").set((float)((centerY - state.centerY()) / state.radius()));
            // Unit-shape coordinates; used only for visibility and the thin silhouette highlight.
            shader.safeGetUniform("CameraLocalPosition").set(
                    (float)((camera.x - state.centerX()) / state.radius()),
                    (float)((camera.y - centerY) / state.radius()),
                    (float)((camera.z - state.centerZ()) / state.radius()));
            shader.safeGetUniform("ZoneTime").set((Minecraft.getInstance().level.getGameTime() + event.getPartialTick()) / 20f);
            // Vanilla sky brightness includes smooth dawn/dusk and rain/thunder dimming.
            float daylight = (Minecraft.getInstance().level.getSkyDarken(event.getPartialTick()) - 0.2f) / 0.8f;
            shader.safeGetUniform("ZoneDaylight").set(Math.max(0f, Math.min(1f, daylight)));
            mesh.bind();
            mesh.drawWithShader(modelView, event.getProjectionMatrix(), shader);
        } finally {
            VertexBuffer.unbind();
            type.clearRenderState();
        }
    }

    private static void cylinderVertex(BufferBuilder builder, double y, int segment) {
        double angle = segment * Math.PI * 2 / 128;
        builder.vertex(Math.cos(angle), y, Math.sin(angle)).endVertex();
    }

    private static void squarePrism(BufferBuilder builder) {
        double[][] corners = {{-1, -1}, {-1, 1}, {1, 1}, {1, -1}};
        for (int i = 0; i < 4; i++) {
            var a = corners[i];
            var b = corners[(i + 1) % 4];
            builder.vertex(a[0], -1, a[1]).endVertex();
            builder.vertex(a[0], 1, a[1]).endVertex();
            builder.vertex(b[0], 1, b[1]).endVertex();
            builder.vertex(b[0], -1, b[1]).endVertex();
        }
    }

    static void release() {
        MESHES.values().forEach(VertexBuffer::close);
        MESHES.clear();
    }
}
