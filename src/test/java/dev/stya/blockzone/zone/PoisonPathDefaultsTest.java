package dev.stya.blockzone.zone;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PoisonPathDefaultsTest {
    @Test void defaultPathPreservesMapCenterAndEndsAtZeroRadius() {
        var path = PoisonPathDefaults.create(12, 20, 80);
        assertEquals(5, path.circles().size());
        assertEquals(new PoisonPath.Circle(12, 20, 80, 0, 0), path.circles().get(0));
        assertEquals(0, path.circles().get(4).radius());
        assertTrue(path.circles().stream().allMatch(c -> c.x() == 12 && c.z() == 20));
    }
    @Test void defaultTransitionsKeepTheirTiming() {
        assertTrue(PoisonPathDefaults.create(0, 0, 40).circles().stream().skip(1)
                .allMatch(c -> c.waitSeconds() == 30 && c.shrinkSeconds() == 60));
    }
    @Test void defaultPathsCanResolveWithinSmallMaps() {
        var resolved = PoisonPathDefaults.create(5, 5, 5).resolve(BoundaryGeometry.of(0, 0, 10, 10), 0);
        assertEquals(5, resolved.size());
    }
}
