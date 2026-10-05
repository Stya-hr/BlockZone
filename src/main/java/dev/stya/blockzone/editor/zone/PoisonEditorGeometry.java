package dev.stya.blockzone.editor.zone;

import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class PoisonEditorGeometry {
    private PoisonEditorGeometry() {}
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
