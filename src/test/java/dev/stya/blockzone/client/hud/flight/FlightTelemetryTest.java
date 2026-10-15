package dev.stya.blockzone.client.hud.flight;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FlightTelemetryTest {
    @Test void heightUsesSurfaceDistanceEvenBelowSeaLevelAndVelocityUsesPerSecondUnits() {
        var value = FlightTelemetry.measure(40, -60, .3, -1.5, 0);
        assertEquals(100, value.altitude());
        assertEquals(Math.hypot(6, 30), value.speed(), .00001);
        assertEquals(30, value.descent());
        assertEquals(0, FlightTelemetry.measure(-61, -60, 0, .2, 0).altitude());
        assertEquals(0, FlightTelemetry.measure(40, -60, 0, .2, 0).descent());
    }
    @Test void unloadedGroundOrInvalidMotionRemainsUnknown() {
        assertTrue(Double.isNaN(FlightTelemetry.measure(100, Double.NaN, 0, -1, 0).altitude()));
        var value = FlightTelemetry.measure(100, 0, Double.NaN, 0, 0);
        assertEquals(100, value.altitude());
        assertTrue(Double.isNaN(value.speed()));
        assertTrue(Double.isNaN(value.descent()));
    }
}
