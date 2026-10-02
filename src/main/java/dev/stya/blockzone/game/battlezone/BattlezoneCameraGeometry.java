package dev.stya.blockzone.game.battlezone;

import org.joml.Matrix4f;

final class BattlezoneCameraGeometry {
    private BattlezoneCameraGeometry() {
    }

    static Matrix4f viewToWorld(Matrix4f worldView) {
        return new Matrix4f(worldView).invert();
    }
}
