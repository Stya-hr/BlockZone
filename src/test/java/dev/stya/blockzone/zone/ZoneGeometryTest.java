package dev.stya.blockzone.zone;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ZoneGeometryTest {
    @Test void defaultCylinderIgnoresHeightAndIncludesCircularBoundary() {
        var cylinder = new ZoneGeometry(0, 64, 0, 10);
        assertEquals(ZoneShape.CYLINDER, cylinder.shape());
        assertTrue(cylinder.contains(6, 100000, 8));
        assertTrue(cylinder.contains(6, -100000, 8));
        assertFalse(cylinder.contains(10, 64, 10));
        assertFalse(cylinder.contains(10.001, 64, 0));
    }

    @Test void squarePrismIncludesCornersAtEveryHeightButRejectsBeyondSideFaces() {
        var prism = new ZoneGeometry(0, 64, 0, 10, ZoneShape.SQUARE_PRISM);
        assertTrue(prism.contains(10, 74, 10));
        assertTrue(prism.contains(-10, 54, -10));
        assertFalse(prism.contains(10.001, 64, 0));
        assertTrue(prism.contains(0, 100000, 0));
        assertTrue(prism.contains(0, -100000, 0));
        assertFalse(prism.contains(0, 64, -10.001));
        assertTrue(prism.containsHorizontal(10, 10));
        assertEquals(ZoneShape.SQUARE_PRISM, prism.fitInside(BoundaryGeometry.of(-20, -20, 20, 20)).shape());
        for (var shape : ZoneShape.values()) assertFalse(new ZoneGeometry(0, 0, 0, 0, shape).contains(0, 0, 0));
    }

    @Test void outOfBoundsCirclesAreTranslatedWithoutChangingRadius() {
        var bounds = BoundaryGeometry.of(99, 49, -100, -50);
        var original = new ZoneGeometry(500, 64, -500, 20);
        var fitted = original.fitInside(bounds);
        assertEquals(new ZoneGeometry(80, 64, -30, 20), fitted);
        assertEquals(500, original.centerX());
        assertEquals(new ZoneGeometry(50, 64, 0, 50), new ZoneGeometry(80, 64, 90, 50).fitInside(bounds));
        assertEquals(new ZoneGeometry(100, 64, -50, 0), new ZoneGeometry(500, 64, -500, 0).fitInside(bounds));
        assertThrows(IllegalArgumentException.class, () -> new ZoneGeometry(0, 64, 0, 51).fitInside(bounds));
    }

    @Test void interpolatedShrinkingCirclesRemainInsideMap() {
        var bounds = BoundaryGeometry.of(-100, -50, 99, 49);
        var start = new ZoneGeometry(-200, 64, 500, 50).fitInside(bounds);
        var end = new ZoneGeometry(200, 64, -500, 10).fitInside(bounds);
        for (int tick = 0; tick <= 100; tick++) {
            double t = tick / 100.0;
            var zone = new ZoneGeometry(start.centerX() + t * (end.centerX() - start.centerX()), 64,
                    start.centerZ() + t * (end.centerZ() - start.centerZ()), start.radius() + t * (end.radius() - start.radius()));
            assertEquals(zone.centerX(), zone.fitInside(bounds).centerX(), 1e-10);
            assertEquals(zone.centerZ(), zone.fitInside(bounds).centerZ(), 1e-10);
            assertEquals(zone.radius(), zone.fitInside(bounds).radius());
        }
    }

    private final ZoneGeometry zone = new ZoneGeometry(100, 64, -100, 50, ZoneShape.CYLINDER);

    @Test void deploymentAndDamageUseTheSameHorizontalFootprint() {
        assertTrue(zone.containsHorizontal(130, -100));
        assertTrue(zone.contains(130, 200, -100));
        assertTrue(zone.contains(130, -200, -100));
        assertTrue(zone.containsHorizontal(150, -100));
        assertFalse(zone.containsHorizontal(151, -100));
        assertFalse(new ZoneGeometry(0, 0, 0, 0).containsHorizontal(0, 0));
    }

    @Test
    void cylinderIncludesBoundaryAndRejectsOutside() {
        assertTrue(zone.contains(150, 64, -100));
        assertFalse(zone.contains(150.001, 64, -100));
    }

    @Test
    void collapsedZoneHasNoImmortalCentrePoint() {
        assertFalse(new ZoneGeometry(100, 64, -100, 0).contains(100, 64, -100));
        assertFalse(new ZoneGeometry(100, 64, -100, -1).contains(100, 64, -100));
    }

    @Test
    void centreHeightIsIndependentOfAreaCornerOrder() {
        assertEquals(64, ZoneGeometry.centerY(64, 128));
        assertEquals(64, ZoneGeometry.centerY(128, 64));
    }
}
