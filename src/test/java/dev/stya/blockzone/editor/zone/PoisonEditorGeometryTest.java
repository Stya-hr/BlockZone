package dev.stya.blockzone.editor.zone;

import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PoisonEditorGeometryTest {
    private final Vec3 eye = new Vec3(0, 100, 0);
    private Quaternionf lookingDown() { return new Quaternionf().rotationYXZ(0, (float)Math.PI / 4, 0); }
    @Test void centerCursorHitsGroundInFrontOfDownwardCamera() {
        var point = PoisonEditorGeometry.ground(eye, lookingDown(), 70, 16.0/9, 0, 0, 0);
        assertNotNull(point); assertEquals(0, point.y, .001); assertEquals(100, point.z, .001);
    }
    @Test void screenRightIsOppositeMinecraftCameraLeft() {
        var point = PoisonEditorGeometry.ground(eye, lookingDown(), 70, 16.0/9, .5, 0, 0);
        assertNotNull(point); assertTrue(point.x < 0);
    }
    @Test void horizontalAndUpwardRaysCannotMoveTheCircleBehindCamera() {
        assertNull(PoisonEditorGeometry.ground(eye, new Quaternionf(), 70, 1, 0, 0, 0));
        assertNull(PoisonEditorGeometry.ground(eye, new Quaternionf().rotationX(-(float)Math.PI/4), 70, 1, 0, 0, 0));
    }
}
