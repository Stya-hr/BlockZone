package dev.stya.blockzone.map.battlezone;

import org.junit.jupiter.api.Test;
import dev.stya.blockzone.util.battlezone.ZoneGeometry;
import static org.junit.jupiter.api.Assertions.*;

class FlightRouteTest {
    @Test void randomRoutesKeepAreaDifferenceBelowEightPercentRegardlessOfHeight() {
        var zone = new ZoneGeometry(100, 0, 100, 100);
        for (int i = 0; i < 360; i++) {
            for (double height : new double[]{16, 64, 200}) {
                for (double offset : new double[]{-.03, 0, .03}) {
                    double angle = Math.toRadians(i);
                    var route = FlightRoute.generateAcrossCircle(zone, .31, 199.69, .31, 199.69,
                            16, height, 20, angle, offset).orElseThrow();
                    assertTrue(route.isValid(.31, 199.69, .31, 199.69, 16));
                    assertEquals(20, route.speed());
                    assertEquals(height, route.startY());
                    assertEquals(height, route.endY());
                    assertTrue(zone.containsHorizontal(route.startX(), route.startZ()));
                    assertTrue(zone.containsHorizontal(route.endX(), route.endZ()));
                    double distance = Math.abs((route.startX() - zone.centerX()) * Math.sin(angle)
                            - (route.startZ() - zone.centerZ()) * Math.cos(angle));
                    assertEquals(Math.abs(offset) * zone.radius(), distance, 1e-8);
                    double radius = zone.radius();
                    double smallerArea = radius * radius * Math.acos(distance / radius)
                            - distance * Math.sqrt(radius * radius - distance * distance);
                    double largerArea = Math.PI * radius * radius - smallerArea;
                    assertTrue((largerArea - smallerArea) / smallerArea <= .08);
                }
            }
        }
    }

    @Test void configuredHeightIsIndependentOfSphereHeight() {
        var route = FlightRoute.generateAcrossCircle(new ZoneGeometry(0, 0, 0, 10),
                -10, 10, -10, 10, 16, 200, 10, 0, 0).orElseThrow();
        assertEquals(200, route.startY());
    }

    @Test void invalidHeightSpeedOrCircleCannotGenerateRoute() {
        var zone = new ZoneGeometry(0, 0, 0, 10);
        assertTrue(FlightRoute.generateAcrossCircle(zone, -10, 10, -10, 10,
                1, 2, 101, 0, 0).isEmpty());
        assertTrue(FlightRoute.generateAcrossCircle(zone, -10, 10, -10, 10,
                5, 2, 20, 0, 0).isEmpty());
        assertTrue(FlightRoute.generateAcrossCircle(zone, -10, 10, -10, 10,
                1, 2000, 20, 0, 0).isEmpty());
        assertTrue(FlightRoute.generateAcrossCircle(new ZoneGeometry(0, 0, 0, 0), -10, 10, -10, 10,
                1, 200, 20, 0, 0).isEmpty());
    }

    @Test void speedUsesBlocksPerSecondAndStopsExactlyAtEndpoint() {
        var route = new FlightRoute(0, 200, 0, 30, 200, 40, 10);
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
        assertTrue(valid(new FlightRoute(1, 200, 1, 99, 200, 99, 20)));
        assertFalse(valid(new FlightRoute(1, 200, 1, 99, 201, 99, 20)));
        assertFalse(valid(new FlightRoute(1, 100, 1, 99, 100, 99, 20)));
        assertFalse(valid(new FlightRoute(-1, 200, 1, 99, 200, 99, 20)));
        assertFalse(valid(new FlightRoute(1, 200, 1, 1, 200, 1, 20)));
        for (double speed : new double[]{0, -1, Double.NaN, Double.POSITIVE_INFINITY, 101}) {
            assertFalse(valid(new FlightRoute(1, 200, 1, 99, 200, 99, speed)));
        }
        assertFalse(valid(new FlightRoute(Double.NaN, 200, 1, 99, 200, 99, 20)));
    }

    @Test void reverseRouteWorksAtLargeWorldCoordinatesAndFractionalSpeeds() {
        var route = new FlightRoute(29_999_990, 300, 10, 29_999_980, 300, 10, 0.5);
        assertEquals(29_999_985, route.x(200), 1e-8);
        assertEquals(10, route.z(200));
        assertEquals(1, route.progress(400));
    }

    private boolean valid(FlightRoute route) {
        return route.isValid(0, 100, 0, 100, 150);
    }
}
