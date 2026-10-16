package dev.stya.blockzone.client.hud.squad;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ZoneStatusIndicatorTest {
    @Test void countdownRoundsUpAndDoesNotBecomeNegativeOrInvalid() {
        assertEquals("01:01", ZoneStatusIndicator.formatTime(60.1f));
        assertEquals("01:00", ZoneStatusIndicator.formatTime(60));
        assertEquals("00:01", ZoneStatusIndicator.formatTime(.1f));
        assertEquals("00:00", ZoneStatusIndicator.formatTime(-1));
        assertEquals("00:00", ZoneStatusIndicator.formatTime(Float.NaN));
    }
}
