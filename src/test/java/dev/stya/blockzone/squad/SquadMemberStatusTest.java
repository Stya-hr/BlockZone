package dev.stya.blockzone.squad;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SquadMemberStatusTest {
    private SquadMemberStatus member(boolean online, boolean living, float hp, float max, float armor, int state, int rescue) {
        return new SquadMemberStatus(UUID.randomUUID(), "Player", 1, online, living, hp, max, armor, 50, 3,
                state, 200, 400, rescue, 160, false, false);
    }
    @Test void healthUsesConfiguredMaximumAndArmorFillsSequentialSegments() {
        var m = member(true, true, 75, 100, 65, 0, 0);
        assertEquals(.75f, m.healthFraction());
        assertEquals(1, m.armorFraction(0));
        assertEquals(.3f, m.armorFraction(1), .001);
        assertEquals(0, m.armorFraction(2));
        assertEquals(0, m.armorFraction(3));
    }
    @Test void deadAndOfflineMembersNeverDisplayRestoredEntityHealth() {
        assertEquals(0, member(true, false, 100, 100, 150, 0, 0).healthFraction());
        assertEquals(0, member(false, true, 100, 100, 150, 0, 0).armorFraction(0));
    }
    @Test void rescueProgressReplacesBleedCountdown() {
        var bleed = member(true, true, 50, 100, 0, 1, 0);
        assertTrue(bleed.downed());
        assertEquals(.5f, bleed.downedFraction());
        assertEquals(.25f, member(true, true, 50, 100, 0, 1, 40).downedFraction());
    }
    @Test void invalidVitalsNeverProduceInvalidGeometry() {
        var m = member(true, true, Float.NaN, 0, Float.POSITIVE_INFINITY, 0, 0);
        assertEquals(0, m.healthFraction());
        assertEquals(0, m.armorFraction(0));
        assertEquals(1, m.maxHealth());
    }
}
