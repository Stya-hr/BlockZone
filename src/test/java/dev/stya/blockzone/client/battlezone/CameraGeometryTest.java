package dev.stya.blockzone.client.battlezone;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CameraGeometryTest {
    @Test
    void forwardRayPointsTowardsTheWorldThePlayerSees() {
        assertForward(0, 0, 0, 0, 1);
        assertForward(90, 0, -1, 0, 0);
        assertForward(-90, 0, 1, 0, 0);
        assertForward(180, 0, 0, 0, -1);
        assertForward(0, 90, 0, -1, 0);
        assertForward(0, -90, 0, 1, 0);
    }

    @Test
    void worldPositionRoundTripsThroughPitchYawRollAndProjection() {
        var view = new Matrix4f().rotateZ(0.2F).rotateX(0.4F).rotateY(1.5F);
        var projection = new Matrix4f().perspective(1.2F, 1.7F, .05F, 512F);
        var world = new Vector4f(12, -6, 8, 1);
        var clip = new Matrix4f(projection).mul(view).transform(new Vector4f(world));
        var reconstructed = CameraGeometry.viewToWorld(view)
                .mul(new Matrix4f(projection).invert()).transform(clip);
        reconstructed.div(reconstructed.w);
        assertEquals(world.x, reconstructed.x, .002F);
        assertEquals(world.y, reconstructed.y, .002F);
        assertEquals(world.z, reconstructed.z, .002F);
    }

    private static void assertForward(float yaw, float pitch, float x, float y, float z) {
        Matrix4f view = new Matrix4f().rotateX((float) Math.toRadians(pitch))
                .rotateY((float) Math.toRadians(yaw + 180));
        Vector3f forward = CameraGeometry.viewToWorld(view).transformDirection(new Vector3f(0, 0, -1));
        assertEquals(x, forward.x, 1.0e-5);
        assertEquals(y, forward.y, 1.0e-5);
        assertEquals(z, forward.z, 1.0e-5);
    }
}
