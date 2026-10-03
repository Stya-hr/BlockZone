package dev.stya.blockzone.client.battlezone;

import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL20;
import java.nio.FloatBuffer;

public final class BattlezoneMaterialRendering {
    private static boolean active;
    private static float x, y, z, radius;
    private static final FloatBuffer CLIP_TO_WORLD = BufferUtils.createFloatBuffer(16);
    private BattlezoneMaterialRendering() {}

    static void begin(RenderLevelStageEvent event, BattlezoneClientState.Snapshot state) {
        active = state != null && state.whiteoutActive() && Minecraft.getInstance().level != null
                && Minecraft.getInstance().level.dimension().location().equals(state.dimension());
        if (!active) return;
        var camera = event.getCamera().getPosition();
        x = (float)(state.centerX() - camera.x);
        y = (float)(state.centerY() - camera.y);
        z = (float)(state.centerZ() - camera.z);
        radius = state.radius();
        CLIP_TO_WORLD.clear();
        BattlezoneCameraGeometry.viewToWorld(event.getPoseStack().last().pose())
                .mul(new Matrix4f(event.getProjectionMatrix()).invert()).get(CLIP_TO_WORLD);
    }

    static void end() { active = false; }

    public static final class Bindings {
        private final int enabled, center, zoneRadius, transform;
        public Bindings(int program) {
            enabled = GL20.glGetUniformLocation(program, "BlockzoneMaterialsActive");
            center = GL20.glGetUniformLocation(program, "BlockzoneZoneCenter");
            zoneRadius = GL20.glGetUniformLocation(program, "BlockzoneZoneRadius");
            transform = GL20.glGetUniformLocation(program, "BlockzoneClipToWorld");
        }
        public void apply() {
            if (enabled < 0) return;
            GL20.glUniform1f(enabled, active ? 1 : 0);
            if (!active) return;
            GL20.glUniform3f(center, x, y, z);
            GL20.glUniform1f(zoneRadius, radius);
            GL20.glUniformMatrix4fv(transform, false, CLIP_TO_WORLD);
        }
    }
}
