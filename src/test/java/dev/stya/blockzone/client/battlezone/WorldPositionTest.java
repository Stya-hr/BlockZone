package dev.stya.blockzone.client.battlezone;

import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WorldPositionTest {
    @Test void materialWorldPositionSurvivesCameraRotationAndProjection() {
        Matrix4f view = new Matrix4f().rotateX(0.4f).rotateY(2.7f).rotateZ(0.1f);
        Matrix4f projection = new Matrix4f().perspective(1.2f, 1.7f, 0.05f, 512f);
        Vector4f position = new Vector4f(12, -3, 18, 1);
        Vector4f clip = new Matrix4f(projection).mul(view).transform(new Vector4f(position));
        Vector4f world = CameraGeometry.viewToWorld(view)
                .mul(new Matrix4f(projection).invert()).transform(clip);
        world.div(world.w);
        assertEquals(position.x, world.x, 0.002f);
        assertEquals(position.y, world.y, 0.002f);
        assertEquals(position.z, world.z, 0.002f);
    }

    @Test void sphereTranslationStaysFixedWhenCameraMovesAtLargeWorldCoordinates() {
        double center = 29_999_980.25;
        for (double camera : new double[]{29_999_970.5, 29_999_985.75}) {
            // Subtract in double precision before passing the relative offset to OpenGL.
            float offset = (float)(center - camera);
            assertEquals(center, camera + offset, 0.00001);
        }
    }
}
