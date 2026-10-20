package dev.stya.blockzone.client.compat;

import dev.stya.blockzone.squad.SquadMemberStatus;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class XaeroMinimapCompatTest {
    @Test void viewClampsAtEdgesAndCentersMapsSmallerThanTheCappedSpan() {
        assertEquals(500, XaeroMinimapCompat.viewCenter(500, 0, 1000, 256));
        assertEquals(128, XaeroMinimapCompat.viewCenter(5, 0, 1000, 256));
        assertEquals(872, XaeroMinimapCompat.viewCenter(995, 0, 1000, 256));
        assertEquals(80, XaeroMinimapCompat.viewCenter(5, 0, 160, 256));
        assertEquals(80, XaeroMinimapCompat.viewCenter(155, 0, 160, 256));
        assertEquals(-872, XaeroMinimapCompat.viewCenter(-995, -1000, 0, 256));
        assertEquals(128, XaeroMinimapCompat.viewCenter(255, 0, 256, 256));
    }
    @Test void mapZoomFillsSmallMapsButKeepsLargeMapsAndMagnificationBounded() {
        assertEquals(256, XaeroMinimapCompat.viewSpan(4000, 4000));
        assertEquals(160, XaeroMinimapCompat.viewSpan(160, 160));
        assertEquals(160, XaeroMinimapCompat.viewSpan(1000, 160));
        assertEquals(128, XaeroMinimapCompat.viewSpan(64, 64));
        assertEquals(80, XaeroMinimapCompat.viewCenter(20, 0, 160, 160));
        assertEquals(80, XaeroMinimapCompat.viewCenter(20, 0, 1000, 160));
        assertEquals(920, XaeroMinimapCompat.viewCenter(980, 0, 1000, 160));
    }
    @Test void mapOnlyExposesLivingOnlineMembersOfTheCurrentRoster() {
        UUID ally = UUID.randomUUID(), enemy = UUID.randomUUID();
        assertTrue(XaeroMinimapCompat.visiblePlayer(ally, List.of(member(ally, true, true))));
        assertFalse(XaeroMinimapCompat.visiblePlayer(enemy, List.of(member(ally, true, true))));
        assertFalse(XaeroMinimapCompat.visiblePlayer(ally, List.of(member(ally, false, true))));
        assertFalse(XaeroMinimapCompat.visiblePlayer(ally, List.of(member(ally, true, false))));
        assertFalse(XaeroMinimapCompat.visiblePlayer(ally, List.of()));
    }
    private static SquadMemberStatus member(UUID id, boolean online, boolean living) {
        return new SquadMemberStatus(id, "Ally", 1, online, living, 100, 100, 0, 50, 2,
                0, 0, 0, 0, 0, false, false);
    }
}
