package dev.stya.blockzone.combat;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CombatRecoveryTest {
    @Test void waitsFiveSecondsThenHealsEveryQuarterSecond() {
        CombatRecovery recovery = new CombatRecovery(100, 5);
        for (int i = 0; i < 104; i++) assertFalse(recovery.tick());
        assertTrue(recovery.tick());
        for (int i = 0; i < 4; i++) assertFalse(recovery.tick());
        assertTrue(recovery.tick());
    }

    @Test void hitRestartsEntireDelayIncludingArmorOnlyHits() {
        CombatRecovery recovery = new CombatRecovery(100, 5);
        for (int i = 0; i < 103; i++) recovery.tick();
        recovery.hurt();
        for (int i = 0; i < 104; i++) assertFalse(recovery.tick());
        assertTrue(recovery.tick());
        recovery.hurt();
        assertFalse(recovery.tick());
    }
    @Test void customDelayIntervalAndZeroDelayAreHonored() {
        var recovery = new CombatRecovery(3, 2);
        for (int i = 0; i < 4; i++) assertFalse(recovery.tick());
        assertTrue(recovery.tick());
        assertFalse(recovery.tick()); assertTrue(recovery.tick());
        recovery.hurt();
        for (int i = 0; i < 4; i++) assertFalse(recovery.tick());
        assertTrue(recovery.tick());
        var immediate = new CombatRecovery(0, 1);
        assertTrue(immediate.tick()); assertTrue(immediate.tick());
    }
}
