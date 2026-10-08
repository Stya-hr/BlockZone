package dev.stya.blockzone.deployment;

import org.junit.jupiter.api.Test;
import java.util.HashSet;
import static org.junit.jupiter.api.Assertions.*;

class ParachuteAppearanceTest {
    @Test
    void teammatesAndReturningPlayersKeepTheirStyle() {
        var appearances = new ParachuteAppearance();
        int first = appearances.forTeam("squad_1");
        appearances.forTeam("squad_2");
        assertEquals(first, appearances.forTeam("squad_1"));
    }

    @Test
    void stylesDoNotRepeatUntilThePaletteIsExhausted() {
        var appearances = new ParachuteAppearance();
        var styles = new HashSet<Integer>();
        var colors = new HashSet<Integer>();
        for (int i = 0; i < ParachuteAppearance.STYLE_COUNT; i++) {
            int style = appearances.forTeam("squad_" + i);
            assertTrue(styles.add(style));
            if (i < 12) assertTrue(colors.add(ParachuteAppearance.color(style)));
        }
    }

    @Test
    void resetStartsAFreshAllocation() {
        var appearances = new ParachuteAppearance();
        appearances.forTeam("old_team");
        appearances.reset();
        var styles = new HashSet<Integer>();
        for (int i = 0; i < ParachuteAppearance.STYLE_COUNT; i++) {
            assertTrue(styles.add(appearances.forTeam("new_team_" + i)));
        }
    }
}
