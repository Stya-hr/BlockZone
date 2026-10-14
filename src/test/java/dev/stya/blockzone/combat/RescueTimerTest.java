package dev.stya.blockzone.combat;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RescueTimerTest {
    @Test void uninterruptedRescueRequiresEightSeconds() {
        var timer = new RescueTimer(1200, 160);
        for (int i = 0; i < 159; i++) { assertFalse(timer.tick(true)); assertFalse(timer.rescued()); }
        assertFalse(timer.tick(true)); assertTrue(timer.rescued());
    }
    @Test void movingAwayOrDamageLosesProgress() {
        var timer = new RescueTimer(1200, 160);
        for (int i = 0; i < 100; i++) timer.tick(true);
        timer.tick(false); assertEquals(0, timer.progress());
        timer.tick(true); timer.interrupt(); assertEquals(0, timer.progress());
        assertEquals(1098, timer.remaining());
    }
    @Test void rescueDoesNotStopBleeding() {
        var timer = new RescueTimer(1200, 160);
        for (int i = 0; i < 1199; i++) assertFalse(timer.tick(i % 2 == 0));
        assertTrue(timer.tick(true)); assertEquals(0, timer.remaining()); assertFalse(timer.rescued());
    }
    @Test void customDurationsDriveBothIndependentClocks() {
        var timer = new RescueTimer(30, 4);
        assertEquals(30, timer.bleedTicks());
        assertEquals(4, timer.rescueTicks());
        for (int i = 0; i < 3; i++) { assertFalse(timer.tick(true)); assertFalse(timer.rescued()); }
        assertFalse(timer.tick(true)); assertTrue(timer.rescued());
        assertEquals(26, timer.remaining());
        timer.interrupt(); assertFalse(timer.rescued());
        for (int i = 0; i < 25; i++) assertFalse(timer.tick(false));
        assertTrue(timer.tick(false));
    }
}
