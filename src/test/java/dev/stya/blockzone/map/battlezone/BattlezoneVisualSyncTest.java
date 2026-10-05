package dev.stya.blockzone.map.battlezone;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BattlezoneVisualSyncTest {
    @Test void periodicSyncKeepsTheExistingFiveTickCadence() {
        assertFalse(BattlezoneVisualSync.shouldSync(104, 100, false));
        assertTrue(BattlezoneVisualSync.shouldSync(105, 100, false));
        assertTrue(BattlezoneVisualSync.shouldSync(110, 100, false));
    }

    @Test void lifecycleSyncBypassesCadenceAndInitializesTheFirstUpdate() {
        assertFalse(BattlezoneVisualSync.shouldSync(100, Long.MIN_VALUE, false));
        assertTrue(BattlezoneVisualSync.shouldSync(100, Long.MIN_VALUE, true));
        assertTrue(BattlezoneVisualSync.shouldSync(100, 100, true));
    }
}
