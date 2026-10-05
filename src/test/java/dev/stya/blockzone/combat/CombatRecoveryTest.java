package dev.stya.blockzone.combat;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CombatRecoveryTest {
    @Test void waitsFiveSecondsThenHealsEveryQuarterSecond() {
        CombatRecovery recovery = new CombatRecovery();
        for (int i = 0; i < 104; i++) assertFalse(recovery.tick());
        assertTrue(recovery.tick());
        for (int i = 0; i < 4; i++) assertFalse(recovery.tick());
        assertTrue(recovery.tick());
    }

    @Test void hitRestartsEntireDelayIncludingArmorOnlyHits() {
        CombatRecovery recovery = new CombatRecovery();
        for (int i = 0; i < 103; i++) recovery.tick();
        recovery.hurt();
        for (int i = 0; i < 104; i++) assertFalse(recovery.tick());
        assertTrue(recovery.tick());
        recovery.hurt();
        assertFalse(recovery.tick());
    }

    @Test void customPlatePointsCapAtThreePlates() {
        assertEquals(25, CombatRecovery.insertPlate(0, 25));
        assertEquals(75, CombatRecovery.insertPlate(65, 25));
        assertEquals(75, CombatRecovery.insertPlate(100, 25));
    }

    @Test void plateRepairsPartialArmorAndCapsAtThreePlates() {
        assertEquals(50, CombatRecovery.insertPlate(0, 50));
        assertEquals(57, CombatRecovery.insertPlate(7, 50));
        assertEquals(150, CombatRecovery.insertPlate(127, 50));
        assertEquals(150, CombatRecovery.insertPlate(150, 50));
    }
}
