package dev.stya.blockzone.squad;

import java.util.HashSet;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SquadMemberColorsTest {
    @Test void referenceColorsAreStableAndTheFirstSixtyFourIndicesDoNotRepeat() {
        assertEquals(0xFF34C759, SquadMemberColors.color(1));
        assertEquals(0xFF0088FF, SquadMemberColors.color(2));
        assertEquals(0xFFFFCC00, SquadMemberColors.color(3));
        var colors = new HashSet<Integer>();
        for (int i = 1; i <= 64; i++) assertTrue(colors.add(SquadMemberColors.color(i)), "Duplicate color for " + i);
        assertEquals(SquadMemberColors.color(24), SquadMemberColors.color(24));
    }
    @Test void indicesStartAtOne() {
        assertThrows(IllegalArgumentException.class, () -> SquadMemberColors.color(0));
    }
}
