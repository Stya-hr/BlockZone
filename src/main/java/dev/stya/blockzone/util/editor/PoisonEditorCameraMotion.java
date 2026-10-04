package dev.stya.blockzone.util.editor;

import dev.stya.blockzone.util.battlezone.ZoneGeometry;
import net.minecraft.world.phys.Vec3;

/** Independent camera state; never reads or mutates a player entity. */
public final class PoisonEditorCameraMotion {
    private Vec3 previous, position;
    private float yaw, pitch;
    public PoisonEditorCameraMotion(ZoneGeometry circle, double mapTop) { focus(circle, mapTop); }
    public void focus(ZoneGeometry circle, double mapTop) {
        double distance = Math.max(32, circle.radius() * 1.25);
        double height = Math.max(mapTop + 24, circle.centerY() + circle.radius() + distance);
        // Keep the overview above the circle instead of outside the map, where no server chunks may be loaded.
        double offset = Math.max(1, circle.radius() * .25);
        position = previous = new Vec3(circle.centerX(), height, circle.centerZ() + offset);
        yaw = 180;
        pitch = (float)Math.toDegrees(Math.atan2(height-circle.centerY(), offset));
    }
    public Vec3 position(double partialTick) {
        double t = Math.max(0, Math.min(1, partialTick));
        return previous.lerp(position, t);
    }
    public Vec3 position() { return position; }
    public float yaw() { return yaw; }
    public float pitch() { return pitch; }
    public void turn(float yawDelta, float pitchDelta) {
        yaw = (yaw + yawDelta) % 360;
        pitch = Math.max(-89.9F, Math.min(89.9F, pitch + pitchDelta));
    }
    public void move(double forward, double sideways, double vertical, boolean fast) {
        previous = position;
        double radians = Math.toRadians(yaw);
        // Keep WASD movement horizontal even while the overview camera looks steeply down.
        var look = new Vec3(-Math.sin(radians), 0, Math.cos(radians));
        var right = new Vec3(Math.cos(radians), 0, Math.sin(radians));
        var movement = look.scale(forward).add(right.scale(sideways)).add(0, vertical, 0);
        // Normalize diagonals; partial combinations with look/up cannot make movement faster.
        if (movement.lengthSqr() > 1) movement = movement.normalize();
        position = position.add(movement.scale(fast ? 6 : 1.5));
    }
}
