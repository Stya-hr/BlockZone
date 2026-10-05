package dev.stya.blockzone.util.editor;

import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LootEditorProjectionTest {
    @Test void cursorGroundRayProjectsBackToTheSamePixelAfterCameraRotation() {
        var origin = new Vec3(200, 70, -150);
        var rotation = new Quaternionf().rotationYXZ(1.3f, 1.1f, 0);
        double screenX = -.6, screenY = .15;
        var world = PoisonEditorGeometry.ground(origin, rotation, 70, 16.0 / 9, screenX, screenY, -60);
        assertNotNull(world);
        var point = LootEditorProjection.project(world, origin, rotation, 70, 960, 540);
        assertNotNull(point);
        assertEquals((screenX + 1) * 480, point.x(), .001);
        assertEquals((1 - screenY) * 270, point.y(), .001);
    }
    @Test void clickRayPassesThroughTheProjectedCrateModel() {
        var origin = new Vec3(250, 40, 210);
        var rotation = new Quaternionf().rotationYXZ(2.7f, .8f, 0);
        var direction = LootEditorProjection.ray(rotation, 70, 960, 540, 200, 160);
        var crate = origin.add(direction.scale(50));
        var pixel = LootEditorProjection.project(crate, origin, rotation, 70, 960, 540);
        assertNotNull(pixel); assertEquals(200, pixel.x(), .001); assertEquals(160, pixel.y(), .001);
        assertEquals(1, direction.length(), .00001);
    }
    @Test void objectsBehindTheCameraCannotBeSelected() {
        assertNull(LootEditorProjection.project(new Vec3(0, 0, -10), Vec3.ZERO, new Quaternionf(), 70, 960, 540));
        assertNull(LootEditorProjection.project(Vec3.ZERO, Vec3.ZERO, new Quaternionf(), 70, 960, 540));
    }
    @Test void worldSelectionDistinguishesCratesOnDifferentFloors() {
        var lower = new LootCrateEdit(1, 2, 3, "demo:a", 0, false);
        var upper = new LootCrateEdit(1, 12, 3, "demo:a", 0, false);
        var draft = new LootCrateDraft(java.util.List.of(lower, upper));
        draft.selectPositions(java.util.Set.of(upper.position()), false);
        assertFalse(draft.selected(lower)); assertTrue(draft.selected(upper));
        draft.selectPositions(java.util.Set.of(lower.position()), true); assertEquals(2, draft.selectedCount());
        draft.filter("12"); draft.selectPositions(java.util.Set.of(lower.position()), false);
        assertEquals(0, draft.selectedCount());
    }
}
