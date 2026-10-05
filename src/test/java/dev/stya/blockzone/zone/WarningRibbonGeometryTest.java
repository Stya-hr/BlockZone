package dev.stya.blockzone.zone;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WarningRibbonGeometryTest {
    @Test void revealsBeforeContactOnBothSidesUsingCollisionBox() {
        assertEquals(1f, WarningRibbonGeometry.proximityAlpha(0, 1, 1.6));
        assertEquals(1f, WarningRibbonGeometry.proximityAlpha(16, 14.4, 15));
        float partial = WarningRibbonGeometry.proximityAlpha(0, 1.6, 2.2);
        assertTrue(partial > 0 && partial < 1);
        assertEquals(0f, WarningRibbonGeometry.proximityAlpha(0, 2, 2.6));
        assertEquals(0f, WarningRibbonGeometry.proximityAlpha(16, 12.4, 13));
    }
    @Test void contactAndCrossingAreFullyVisible() {
        assertEquals(1f, WarningRibbonGeometry.proximityAlpha(0, -0.2, 0.4));
        assertEquals(1f, WarningRibbonGeometry.proximityAlpha(0, 0, 0.6));
    }
    @Test void bothEndsFadeSymmetricallyAndMiddleStaysContinuous() {
        assertEquals(1f, WarningRibbonGeometry.endAlpha(24, 24));
        assertEquals(0f, WarningRibbonGeometry.endAlpha(12, 24));
        assertEquals(0f, WarningRibbonGeometry.endAlpha(36, 24));
        assertEquals(0.5f, WarningRibbonGeometry.endAlpha(13, 24));
        assertEquals(WarningRibbonGeometry.endAlpha(13, 24), WarningRibbonGeometry.endAlpha(35, 24));
    }
}
