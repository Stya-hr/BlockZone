package dev.stya.blockzone.client.hud.notice;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BriefGuidanceTest {
    @Test void waitsForLandingThenShowsOnceAndResetsForNextMatch() {
        var brief = new BriefGuidance();
        assertFalse(brief.visible("round1", false, 0));
        assertFalse(brief.visible("round1", false, 10000));
        assertTrue(brief.visible("round1", true, 20000));
        assertTrue(brief.visible("round1", true, 23999));
        assertFalse(brief.visible("round1", true, 24000));
        assertFalse(brief.visible("round1", false, 25000));
        assertFalse(brief.visible("round1", true, 26000));
        assertTrue(brief.visible("round2", true, 30000));
    }
}
