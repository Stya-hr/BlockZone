package dev.stya.blockzone.client.hud.squad;

import java.util.HashSet;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SquadRosterLayoutTest {
    @Test void referenceThreePersonSquadKeepsOriginalCoordinates() {
        var l = SquadRosterLayout.create(2, 1256, 2560);
        assertEquals(1, l.pages()); assertEquals(0, l.x(0));
        assertEquals(-42, l.y(0)); assertEquals(-95, l.y(1));
    }
    @Test void configurableTwentyFourPersonSquadUsesASecondColumnWithoutShrinkingRows() {
        var l = SquadRosterLayout.create(23, 1256, 2560);
        assertEquals(3, l.pages()); assertEquals(2, l.columns());
        assertEquals(4, l.rows()); assertEquals(8, l.capacity());
        assertEquals(53, l.y(0) - l.y(1));
        assertEquals(220, l.x(l.rows())); assertEquals(-42, l.y(l.rows()));
    }
    @Test void fifthTeammateStartsSecondColumnAndNinthStartsNextPage() {
        var four = SquadRosterLayout.create(4, 1256, 2560);
        assertEquals(0, four.x(3));
        var five = SquadRosterLayout.create(5, 1256, 2560);
        assertEquals(220, five.x(4)); assertEquals(-42, five.y(4));
        assertEquals(1, SquadRosterLayout.create(8, 1256, 2560).pages());
        var nine = SquadRosterLayout.create(9, 1256, 2560);
        assertEquals(2, nine.pages()); assertEquals(8, nine.start(1));
        assertEquals(9, nine.end(1));
    }
    @Test void everyMemberOfALargeSquadCanBeReachedWithNoOverlapOrLostEntries() {
        var l = SquadRosterLayout.create(100, 1256, 2560);
        var seen = new HashSet<Integer>();
        for (int page = 0; page < l.pages(); page++) {
            var positions = new HashSet<String>();
            for (int i = l.start(page); i < l.end(page); i++) {
                assertTrue(seen.add(i));
                int slot = i - l.start(page);
                assertTrue(positions.add(l.x(slot) + ":" + l.y(slot)));
                assertTrue(1256 + l.y(slot) >= SquadRosterLayout.TOP_SAFE);
            }
        }
        assertEquals(100, seen.size());
    }
    @Test void emptyAndReducedRostersHaveAValidFirstPage() {
        var l = SquadRosterLayout.create(0, 1256, 2560);
        assertEquals(1, l.pages()); assertEquals(0, l.end(50));
        assertEquals(0, l.start(50));
    }
}
