package dev.stya.blockzone.client.hud.common;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HudProgressTest {
    @Test void elapsedAndBleedClocksShareSecondsButUseOppositeFillDirections() {
        assertEquals(new HudProgress(.25f, 6), HudProgress.elapsed(40, 160, 20));
        assertEquals(new HudProgress(.75f, 6), HudProgress.remaining(120, 160, 20));
        assertEquals(new HudProgress(0, 2), HudProgress.elapsed(0, 40, 20));
        assertEquals(new HudProgress(1, 0), HudProgress.elapsed(60, 40, 20));
        assertEquals(new HudProgress(0, 0), HudProgress.remaining(-5, 40, 20));
    }
    @Test void missingOrInvalidDurationsDoNotProduceInvalidGeometry() {
        assertEquals(new HudProgress(0, 0), HudProgress.elapsed(10, 0, 20));
        assertEquals(new HudProgress(0, 0), HudProgress.remaining(10, 0, 20));
        assertEquals(new HudProgress(0, 0), HudProgress.remaining(Double.NaN, 40, 20));
        assertEquals(new HudProgress(0, 0), HudProgress.elapsed(0, 40, 0));
    }
}
