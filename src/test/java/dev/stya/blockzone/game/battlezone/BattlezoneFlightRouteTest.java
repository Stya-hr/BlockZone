package dev.stya.blockzone.game.battlezone;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BattlezoneFlightRouteTest {
    @Test void speedUsesBlocksPerSecondAndStopsExactlyAtEndpoint() {
        var route = new BattlezoneFlightRoute(0, 200, 0, 30, 200, 40, 10);
        assertEquals(50, route.length());
        assertEquals(0.2, route.progress(20), 1e-9);
        assertEquals(6, route.x(20), 1e-9);
        assertEquals(8, route.z(20), 1e-9);
        assertEquals(1, route.progress(100));
        assertEquals(30, route.x(1000));
        assertEquals(40, route.z(1000));
        assertEquals(0, route.progress(-1));
    }

    @Test void validatesHorizontalHighAltitudeRoutesInsideMap() {
        assertTrue(valid(new BattlezoneFlightRoute(1, 200, 1, 99, 200, 99, 20)));
        assertFalse(valid(new BattlezoneFlightRoute(1, 200, 1, 99, 201, 99, 20)));
        assertFalse(valid(new BattlezoneFlightRoute(1, 100, 1, 99, 100, 99, 20)));
        assertFalse(valid(new BattlezoneFlightRoute(-1, 200, 1, 99, 200, 99, 20)));
        assertFalse(valid(new BattlezoneFlightRoute(1, 200, 1, 1, 200, 1, 20)));
        for (double speed : new double[]{0, -1, Double.NaN, Double.POSITIVE_INFINITY, 101}) {
            assertFalse(valid(new BattlezoneFlightRoute(1, 200, 1, 99, 200, 99, speed)));
        }
        assertFalse(valid(new BattlezoneFlightRoute(Double.NaN, 200, 1, 99, 200, 99, 20)));
    }

    @Test void reverseRouteWorksAtLargeWorldCoordinatesAndFractionalSpeeds() {
        var route = new BattlezoneFlightRoute(29_999_990, 300, 10, 29_999_980, 300, 10, 0.5);
        assertEquals(29_999_985, route.x(200), 1e-8);
        assertEquals(10, route.z(200));
        assertEquals(1, route.progress(400));
    }

    private boolean valid(BattlezoneFlightRoute route) {
        return route.isValid(0, 100, 0, 100, 150);
    }
}
