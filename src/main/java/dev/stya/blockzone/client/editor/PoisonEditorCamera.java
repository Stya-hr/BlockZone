package dev.stya.blockzone.client.editor;

import com.ptcrys.fpsmatch.common.camera.SequenceClock;
import com.ptcrys.fpsmatch.common.client.camera.*;
import dev.stya.blockzone.util.battlezone.ZoneGeometry;
import dev.stya.blockzone.util.editor.PoisonEditorCameraMotion;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import java.util.HashSet;
import java.util.Set;

/** Uses FPSMatch ownership and input policy so editor navigation cannot move the player. */
final class PoisonEditorCamera {
    private static final int PRIORITY = CameraDirector.DEATH_PRIORITY - 1;
    private static final CameraPolicy POLICY = new CameraPolicy(CameraPolicy.LookInput.ORBIT,
            true, true, false, true, false, false, true, true, true);
    private static final Set<Integer> HELD = new HashSet<>();
    private static CameraSession lease;
    private static PoisonEditorCameraMotion motion;
    private static boolean lastWindowActive;
    private PoisonEditorCamera() {}

    static boolean start() {
        stop();
        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.player.isDeadOrDying() || CameraDirector.hasSession() || !CameraDirector.accepts(PRIORITY)) return false;
        var editor = PoisonWorldEditor.session;
        var player = mc.player;
        var movement = new PoisonEditorCameraMotion(selectedCircle(), mapTop());
        motion = movement;
        lastWindowActive = mc.isWindowActive();
        lease = CameraDirector.play("blockzone:poison_editor", PRIORITY, new CameraRig() {
            @Override public CameraFrame sample(double time) {
                return CameraFrame.independent(new CameraPose(movement.position(time - Math.floor(time)), movement.yaw(), movement.pitch()));
            }
            @Override public void turn(float x, float y) { movement.turn(x * .15F, y * .15F); }
        }, POLICY, new SequenceClock(),
                () -> PoisonWorldEditor.session == editor && mc.player == player && !player.isDeadOrDying()
                        && mc.level != null && mc.level.dimension().location().equals(editor.dimension()),
                CameraLifetime.PLAYER_LIFE, reason -> {
                    lease = null; motion = null; clearMovement();
                    if (PoisonWorldEditor.session != null) PoisonWorldEditor.message =
                            net.minecraft.network.chat.Component.translatable("editor.blockzone.camera_ended").getString();
                });
        if (lease == null) motion = null;
        else PoisonWorldEditor.message = "";
        return active();
    }
    static boolean active() { return lease != null && lease.isActive() && motion != null; }
    static void stop() {
        var current = lease;
        lease = null; motion = null; clearMovement();
        if (current != null) current.close();
    }
    static void clearMovement() { HELD.clear(); }
    static void turn(double x, double y) { if (active()) motion.turn((float)x * .45F, (float)y * .45F); }
    static void focus() {
        if (!active() && !start()) {
            PoisonWorldEditor.message = net.minecraft.network.chat.Component.translatable("editor.blockzone.camera_busy").getString();
            return;
        }
        clearMovement(); motion.focus(selectedCircle(), mapTop()); CameraDirector.refresh();
    }
    static net.minecraft.world.phys.Vec3 position() { return active() ? motion.position() : null; }
    private static ZoneGeometry selectedCircle() {
        var c = PoisonWorldEditor.draft.selected();
        return new ZoneGeometry(c.x(), PoisonWorldEditor.y(), c.z(), Math.min(c.radius(), Math.min(PoisonWorldEditor.bounds().maxX()-PoisonWorldEditor.bounds().minX(),
                        PoisonWorldEditor.bounds().maxZ()-PoisonWorldEditor.bounds().minZ())/2), PoisonWorldEditor.draft.path().shape()).fitInside(PoisonWorldEditor.bounds());
    }
    private static double mapTop() { return Math.max(PoisonWorldEditor.session.pos1().getY(), PoisonWorldEditor.session.pos2().getY()); }
    static boolean key(int key, int scan, int action) {
        if (action == org.lwjgl.glfw.GLFW.GLFW_RELEASE) { HELD.remove(key); return false; }
        if (!active()) return false;
        var mc = Minecraft.getInstance();
        if (mc.screen instanceof PoisonEditorScreen screen && screen.getFocused() instanceof EditBox) {
            clearMovement(); return false;
        }
        if (mc.screen != null && !(mc.screen instanceof PoisonEditorScreen)) { clearMovement(); return false; }
        if (mc.options.keyUp.matches(key, scan) || mc.options.keyDown.matches(key, scan)
                || mc.options.keyLeft.matches(key, scan) || mc.options.keyRight.matches(key, scan)
                || mc.options.keyJump.matches(key, scan) || mc.options.keyShift.matches(key, scan)
                || mc.options.keySprint.matches(key, scan)) { HELD.add(key); return true; }
        return false;
    }
    private static int down(KeyMapping mapping) {
        // Key mappings use their configured binding, including remapped movement keys.
        for (int key : HELD) if (mapping.matches(key, 0)) return 1;
        return 0;
    }
    static void tick() {
        if (!active()) { clearMovement(); return; }
        var mc = Minecraft.getInstance();
        if ((mc.screen != null && !(mc.screen instanceof PoisonEditorScreen))
                || (mc.screen instanceof PoisonEditorScreen screen && screen.getFocused() instanceof EditBox)
                || (lastWindowActive && !mc.isWindowActive())) clearMovement();
        lastWindowActive = mc.isWindowActive();
        var options = mc.options;
        motion.move(down(options.keyUp) - down(options.keyDown), down(options.keyRight) - down(options.keyLeft),
                down(options.keyJump) - down(options.keyShift), down(options.keySprint) != 0);
    }
}
