package dev.stya.blockzone.editor.zone;

import dev.stya.blockzone.editor.loot.LootEditorProjection;
import dev.stya.blockzone.zone.ZoneGeometry;
import dev.stya.blockzone.zone.ZoneShape;
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

    @Test void viewportFocusProjectsTheSelectedZoneIntoTheCanvasAcrossWindowSizesAndFovs() {
        var circle = new ZoneGeometry(86, -64, -135, 160, ZoneShape.SQUARE_PRISM);
        for (int[] size : new int[][] {{320, 240}, {426, 240}, {640, 360}}) {
            for (double fov : new double[] {50, 70, 110}) {
                assertViewportFocus(circle, 48, fov, size[0], size[1], (size[0] - 152) / 2.0);
            }
        }
    }

    @Test void viewportFocusUsesTheActualCameraDepthForPointZonesAndTallMaps() {
        var point = new ZoneGeometry(10, 10, 20, 0);
        assertViewportFocus(point, -64, 50, 320, 240, 84);
        assertViewportFocus(point, 512, 90, 426, 240, 137);
        assertViewportFocus(new ZoneGeometry(-240, -64, 30, 80), 1024, 110, 640, 360, 244);
    }

    @Test void screenCenteredViewportsKeepTheOriginalFocusGeometry() {
        var circle = new ZoneGeometry(86, -64, -135, 160, ZoneShape.SQUARE_PRISM);
        for (int[] size : new int[][] {{320, 240}, {426, 240}, {640, 360}}) {
            double centerX = size[0] / 2.0;
            assertEquals(circle, PoisonEditorGeometry.focusForViewport(circle, 48, 70,
                    size[0], size[1], centerX));
            assertViewportFocus(circle, 48, 70, size[0], size[1], centerX);
        }
    }

    @Test void anUninitializedWindowDoesNotCreateAnInvalidFocusTarget() {
        var circle = new ZoneGeometry(10, 10, 20, 80);
        assertEquals(circle, PoisonEditorGeometry.focusForViewport(circle, 100, 70, 0, 240, 80));
        assertEquals(circle, PoisonEditorGeometry.focusForViewport(circle, 100, 70, 320, 0, 80));
    }

    private void assertViewportFocus(ZoneGeometry circle, double mapTop, double fov,
            int width, int height, double viewportCenterX) {
        var target = PoisonEditorGeometry.focusForViewport(circle, mapTop, fov, width, height, viewportCenterX);
        assertEquals(circle.centerY(), target.centerY());
        assertEquals(circle.centerZ(), target.centerZ());
        assertEquals(circle.radius(), target.radius());
        assertEquals(circle.shape(), target.shape());

        var camera = new PoisonEditorCameraMotion(target, mapTop);
        var rotation = new Quaternionf().rotationYXZ((float)Math.toRadians(-camera.yaw()),
                (float)Math.toRadians(camera.pitch()), 0);
        var pixel = LootEditorProjection.project(new Vec3(circle.centerX(), circle.centerY(), circle.centerZ()),
                camera.position(), rotation, fov, width, height);
        String description = width + "x" + height + ", FOV " + fov + ", map top " + mapTop;
        assertNotNull(pixel, description);
        assertEquals(viewportCenterX, pixel.x(), .001, description);
        assertEquals(height / 2.0, pixel.y(), .001, description);
    }
}
