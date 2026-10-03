package dev.stya.blockzone.util.battlezone;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ZoneGeometryTest {
    private final ZoneGeometry zone = new ZoneGeometry(100, 64, -100, 50);

    @Test
    void highGroundUsesSphereInsteadOfHorizontalCircle() {
        assertTrue(zone.contains(130, 104, -100));
        assertFalse(zone.contains(140, 104, -100));
        assertFalse(zone.contains(100, 115, -100));
    }

    @Test
    void lowerHemisphereIsSafeWithinRadius() {
        assertTrue(zone.contains(130, 24, -100));
        assertFalse(zone.contains(140, 24, -100));
        assertFalse(zone.contains(100, 13, -100));
    }

    @Test
    void equatorIncludesBoundaryAndRejectsOutside() {
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
