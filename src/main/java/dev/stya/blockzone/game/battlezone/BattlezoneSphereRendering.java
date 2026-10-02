package dev.stya.blockzone.game.battlezone;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

final class BattlezoneSphereRendering {
    private static VertexBuffer mesh;
    private BattlezoneSphereRendering() {}

    static void render(RenderLevelStageEvent event, BattlezoneClientState.Snapshot state) {
        if (state.radius() <= 0) return;
        if (mesh == null) {
            BufferBuilder builder = new BufferBuilder(64 * 32 * 4 * 12);
            builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
            for (int lat = 0; lat < 32; lat++) {
                for (int lon = 0; lon < 64; lon++) {
                    vertex(builder, lat, lon);
                    vertex(builder, lat + 1, lon);
                    vertex(builder, lat + 1, lon + 1);
                    vertex(builder, lat, lon + 1);
                }
            }
            mesh = new VertexBuffer(VertexBuffer.Usage.STATIC);
            mesh.bind();
            mesh.upload(builder.end());
            VertexBuffer.unbind();
        }
        var camera = event.getCamera().getPosition();
        Matrix4f modelView = new Matrix4f(event.getPoseStack().last().pose())
                .translate((float)(state.centerX() - camera.x), (float)(state.centerY() - camera.y),
                        (float)(state.centerZ() - camera.z)).scale(state.radius());
        var type = BattlezoneRenderTypes.sphere();
        type.setupRenderState();
        try {
            var shader = BattlezoneRenderTypes.sphereShader();
            shader.safeGetUniform("ZoneTime").set((Minecraft.getInstance().level.getGameTime() + event.getPartialTick()) / 20f);
            mesh.bind();
            mesh.drawWithShader(modelView, event.getProjectionMatrix(), shader);
        } finally {
            VertexBuffer.unbind();
            type.clearRenderState();
        }
    }

    private static void vertex(BufferBuilder builder, int lat, int lon) {
        double latitude = -Math.PI / 2 + Math.PI * lat / 32;
        double longitude = Math.PI * 2 * lon / 64;
        builder.vertex(Math.cos(latitude) * Math.cos(longitude), Math.sin(latitude),
                Math.cos(latitude) * Math.sin(longitude)).endVertex();
    }

    static void release() {
        if (mesh != null) { mesh.close(); mesh = null; }
    }
}
