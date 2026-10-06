package dev.stya.blockzone.combat;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RescueTimerTest {
    @Test void uninterruptedRescueRequiresEightSeconds() {
        var timer = new RescueTimer();
        for (int i = 0; i < 159; i++) { assertFalse(timer.tick(true)); assertFalse(timer.rescued()); }
        assertFalse(timer.tick(true)); assertTrue(timer.rescued());
    }
    @Test void movingAwayOrDamageLosesProgress() {
        var timer = new RescueTimer();
        for (int i = 0; i < 100; i++) timer.tick(true);
        timer.tick(false); assertEquals(0, timer.progress());
        timer.tick(true); timer.interrupt(); assertEquals(0, timer.progress());
        assertEquals(1098, timer.remaining());
    }
    @Test void rescueDoesNotStopBleeding() {
        var timer = new RescueTimer();
        for (int i = 0; i < 1199; i++) assertFalse(timer.tick(i % 2 == 0));
        assertTrue(timer.tick(true)); assertEquals(0, timer.remaining()); assertFalse(timer.rescued());
    }
}
