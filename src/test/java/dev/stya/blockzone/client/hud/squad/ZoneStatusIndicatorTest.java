package dev.stya.blockzone.client.hud.squad;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ZoneStatusIndicatorTest {
    @Test void progressUsesAuthoritativePhaseDurationAndClamps() {
        assertEquals(0, ZoneStatusIndicator.progress(60, 1200));
        assertEquals(.5f, ZoneStatusIndicator.progress(30, 1200));
        assertEquals(1, ZoneStatusIndicator.progress(0, 1200));
        assertEquals(1, ZoneStatusIndicator.progress(-1, 1200));
        assertEquals(0, ZoneStatusIndicator.progress(90, 1200));
        assertEquals(0, ZoneStatusIndicator.progress(Float.NaN, 1200));
        assertEquals(1, ZoneStatusIndicator.progress(0, 0));
    }
    @Test void countdownRoundsUpAndDoesNotBecomeNegativeOrInvalid() {
        assertEquals("01:01", ZoneStatusIndicator.formatTime(60.1f));
        assertEquals("01:00", ZoneStatusIndicator.formatTime(60));
        assertEquals("00:01", ZoneStatusIndicator.formatTime(.1f));
        assertEquals("00:00", ZoneStatusIndicator.formatTime(-1));
        assertEquals("00:00", ZoneStatusIndicator.formatTime(Float.NaN));
    }
}
