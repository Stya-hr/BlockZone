package dev.stya.blockzone.util.editor;

import dev.stya.blockzone.util.battlezone.ZoneGeometry;
import org.joml.Quaternionf;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PoisonEditorCameraMotionTest {
    private PoisonEditorCameraMotion camera() {
        return new PoisonEditorCameraMotion(new ZoneGeometry(80, -64, 80, 80), -24);
    }
    @Test void initialOverviewClearsTheMapAndLooksAtTheSelectedCircle() {
        var camera = camera();
        assertTrue(camera.position().y > 16);
        assertTrue(camera.position().z >= 0 && camera.position().z <= 160);
        var rotation = new Quaternionf().rotationYXZ((float)Math.toRadians(-camera.yaw()), (float)Math.toRadians(camera.pitch()), 0);
        var center = PoisonEditorGeometry.ground(camera.position(), rotation, 70, 16.0/9, 0, 0, -64);
        assertNotNull(center); assertEquals(80, center.x, .001); assertEquals(80, center.z, .001);
        camera.focus(new ZoneGeometry(10, 10, 20, 0), 100);
        assertTrue(camera.position().y > 100);
    }
    @Test void forwardFlightAndVerticalFlightAreIndependentOfGravityAndTerrain() {
        var camera = camera(); var start = camera.position();
        for (int i = 0; i < 20; i++) camera.move(1, 0, 0, false);
        assertEquals(30, start.distanceTo(camera.position()), .001);
        assertTrue(camera.position().y < start.y);
        start = camera.position(); camera.move(0, 0, 1, false);
        assertEquals(start.y + 1.5, camera.position().y, .001);
        assertEquals(start.x, camera.position().x); assertEquals(start.z, camera.position().z);
    }
    @Test void diagonalBoostIsBoundedAndPitchCannotFlipTheView() {
        var camera = camera(); var start = camera.position();
        camera.move(1, 1, -1, true);
        assertTrue(start.distanceTo(camera.position()) <= 6.000001);
        camera.turn(0, 1000); assertEquals(89.9F, camera.pitch());
        camera.turn(0, -2000); assertEquals(-89.9F, camera.pitch());
    }
    @Test void renderInterpolationAndFocusDoNotLeaveAFlyingCameraDrifting() {
        var camera = camera(); var start = camera.position(); camera.move(0, 0, 1, false);
        assertEquals(start, camera.position(0));
        assertEquals(start.y+.75, camera.position(.5).y, .001);
        assertEquals(camera.position(), camera.position(1));
        camera.move(0, 0, 0, false);
        assertEquals(camera.position(), camera.position(0));
        camera.focus(new ZoneGeometry(0, 0, 0, 10), 10);
        assertEquals(camera.position(), camera.position(0));
    }
}
