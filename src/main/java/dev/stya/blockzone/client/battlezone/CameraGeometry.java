package dev.stya.blockzone.client.battlezone;

import org.joml.Matrix4f;

final class CameraGeometry {
    private CameraGeometry() {
    }

    static Matrix4f viewToWorld(Matrix4f worldView) {
        return new Matrix4f(worldView).invert();
    }
}
