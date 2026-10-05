package dev.stya.blockzone.util.editor;

import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Projects crate centers using the same camera axes as the sequence editor. */
public final class LootEditorProjection {
    private LootEditorProjection() {}
    public record Point(double x, double y, double depth) {}
    public static Vec3 ray(Quaternionf rotation, double fov, int width, int height, double x, double y) {
        double tangent = Math.tan(Math.toRadians(fov) / 2);
        var vector = new Vector3f((float)(-(2 * x / width - 1) * tangent * width / height),
                (float)((1 - 2 * y / height) * tangent), 1).rotate(rotation).normalize();
        return new Vec3(vector.x, vector.y, vector.z);
    }
    public static Point project(Vec3 point, Vec3 origin, Quaternionf rotation, double fov,
                                int width, int height) {
        var delta = point.subtract(origin);
        var local = new Vector3f((float)delta.x, (float)delta.y, (float)delta.z)
                .rotate(new Quaternionf(rotation).conjugate());
        if (local.z <= .01f || width <= 0 || height <= 0) return null;
        double tangent = Math.tan(Math.toRadians(fov) / 2);
        return new Point(width * (1 - local.x / (local.z * tangent * width / height)) / 2,
                height * (1 - local.y / (local.z * tangent)) / 2, local.z);
    }
}
