package dev.stya.blockzone.deployment;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FlightTimelineTest {
    @Test void jitteredSnapshotsDoNotJumpOrReverseAndEventuallyConverge() {
        var clock = new FlightTimeline();
        clock.update(0, 100, true);
        double time = 100;
        for (int i = 1; i <= 30; i++) {
            time += i % 2 == 0 ? 4 : 6;
            double before = clock.ticks(time);
            clock.update(i * 5, time, false);
            assertEquals(before, clock.ticks(time), 1e-9);
            assertTrue(clock.ticks(time + .5) > before);
        }
        double before = clock.ticks(time);
        clock.update(100, time, false);
        assertEquals(before, clock.ticks(time), 1e-9);
        assertTrue(clock.ticks(time + 1) > before);
        assertEquals(300, clock.ticks(time + 200), 1e-9);
        clock.update(0, time + 201, true);
        assertEquals(0, clock.ticks(time + 201), 1e-9);
    }
}
