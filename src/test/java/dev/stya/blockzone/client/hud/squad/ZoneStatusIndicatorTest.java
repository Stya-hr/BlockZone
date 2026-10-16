package dev.stya.blockzone.client.hud.squad;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ZoneStatusIndicatorTest {
    @Test void sequenceProgressContinuesAcrossWaitingShrinkingAndStages() {
        var ticks = java.util.List.of(200, 400, 100, 300);
        assertEquals(0, ZoneStatusIndicator.sequenceProgress(1, false, 10, ticks));
        assertEquals(.2f, ZoneStatusIndicator.sequenceProgress(1, true, 20, ticks));
        assertEquals(.4f, ZoneStatusIndicator.sequenceProgress(1, true, 10, ticks));
        assertEquals(.6f, ZoneStatusIndicator.sequenceProgress(2, false, 5, ticks));
        assertEquals(.7f, ZoneStatusIndicator.sequenceProgress(2, true, 15, ticks));
        assertEquals(1, ZoneStatusIndicator.sequenceProgress(3, false, 0, ticks));
        assertEquals(1, ZoneStatusIndicator.sequenceProgress(1, true, 0, java.util.List.of(0, 0)));
    }
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
