package dev.stya.blockzone.map.battlezone;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FlightFormationTest {
    @Test
    void threePlayersFormTriangleBehindLeader() {
        assertEquals(new FlightFormation.Offset(0, 0), FlightFormation.offset(0, 0));
        assertEquals(new FlightFormation.Offset(-3, -3), FlightFormation.offset(1, 0));
        assertEquals(new FlightFormation.Offset(3, -3), FlightFormation.offset(2, 0));
    }

    @Test
    void rotatingRoutePreservesSpacingAndKeepsWingsBehind() {
        for (float yaw : new float[] {0, 90, -90, 180, 37}) {
            var left = FlightFormation.offset(1, yaw);
            var right = FlightFormation.offset(2, yaw);
            assertEquals(6, Math.hypot(left.x() - right.x(), left.z() - right.z()), 1e-9);
            double angle = Math.toRadians(yaw);
            assertEquals(-3, -Math.sin(angle) * left.x() + Math.cos(angle) * left.z(), 1e-9);
            assertEquals(-3, -Math.sin(angle) * right.x() + Math.cos(angle) * right.z(), 1e-9);
        }
    }
}
