package dev.stya.blockzone.map.battlezone;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BattlezonePresentationSyncTest {
    @Test void periodicSyncKeepsTheExistingFiveTickCadence() {
        assertFalse(BattlezonePresentationSync.shouldSync(104, 100, false));
        assertTrue(BattlezonePresentationSync.shouldSync(105, 100, false));
        assertTrue(BattlezonePresentationSync.shouldSync(110, 100, false));
    }

    @Test void lifecycleSyncBypassesCadenceAndInitializesTheFirstUpdate() {
        assertFalse(BattlezonePresentationSync.shouldSync(100, Long.MIN_VALUE, false));
        assertTrue(BattlezonePresentationSync.shouldSync(100, Long.MIN_VALUE, true));
        assertTrue(BattlezonePresentationSync.shouldSync(100, 100, true));
    }
}
