package dev.stya.blockzone.editor.zone;

import dev.stya.blockzone.zone.ZoneGeometry;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class PoisonEditorGeometry {
    private PoisonEditorGeometry() {}

    /** Returns a virtual camera target that centers the unchanged zone in the visible viewport. */
    public static ZoneGeometry focusForViewport(ZoneGeometry circle, double mapTop, double fovDegrees,
            int screenWidth, int screenHeight, double viewportCenterX) {
        if (screenWidth <= 0 || screenHeight <= 0) return circle;
        double height = Math.max(mapTop + 24,
                circle.centerY() + circle.radius() + Math.max(32, circle.radius() * 1.25));
        double zOffset = Math.max(1, circle.radius() * .25);
        // Include the Z offset of PoisonEditorCameraMotion.focus in the tilted overview's depth.
        double depth = Math.hypot(height - circle.centerY(), zOffset);
        double tangent = Math.tan(Math.toRadians(fovDegrees) / 2);
        double xOffset = (screenWidth / 2.0 - viewportCenterX) * 2 * depth * tangent / screenHeight;
        return new ZoneGeometry(circle.centerX() + xOffset, circle.centerY(), circle.centerZ(),
                circle.radius(), circle.shape());
    }

    /** Minecraft camera space looks along +Z, with +X pointing left. */
    public static Vec3 ground(Vec3 origin, Quaternionf rotation, double fovDegrees,
            double aspect, double screenX, double screenY, double planeY) {
        double tangent = Math.tan(Math.toRadians(fovDegrees) / 2);
        var ray = new Vector3f((float)(-screenX*tangent*aspect), (float)(screenY*tangent), 1).rotate(rotation);
        if (Math.abs(ray.y) < .0001) return null;
        double distance = (planeY-origin.y)/ray.y;
        if (!Double.isFinite(distance) || distance < 0 || distance > 100000) return null;
        return origin.add(ray.x*distance, ray.y*distance, ray.z*distance);
    }
}
