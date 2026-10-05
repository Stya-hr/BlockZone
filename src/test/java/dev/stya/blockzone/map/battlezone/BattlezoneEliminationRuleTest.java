package dev.stya.blockzone.map.battlezone;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BattlezoneEliminationRuleTest {
    private final BattlezoneEliminationRule rule = new BattlezoneEliminationRule();

    @Test void onlyTheLastLivingTeamWins() {
        assertTrue(rule.evaluate(new BattlezoneMatchContext(false, true, List.of("a", "b"))).isEmpty());
        var result = rule.evaluate(new BattlezoneMatchContext(false, true, List.of("b"))).orElseThrow();
        assertEquals("b", result.winner());
        assertEquals(BattlezoneResultReason.LAST_TEAM_STANDING, result.reason());
    }

    @Test void noLivingTeamsIsADraw() {
        var result = rule.evaluate(new BattlezoneMatchContext(false, true, List.of())).orElseThrow();
        assertNull(result.winner());
        assertEquals(BattlezoneResultReason.DRAW, result.reason());
    }

    @Test void debugAndNonCombatPhasesDoNotEndTheMatch() {
        assertTrue(rule.evaluate(new BattlezoneMatchContext(true, true, List.of("a"))).isEmpty());
        assertTrue(rule.evaluate(new BattlezoneMatchContext(false, false, List.of("a"))).isEmpty());
        assertTrue(rule.evaluate(new BattlezoneMatchContext(false, false, List.of())).isEmpty());
    }

    @Test void ruleInputKeepsItsCapturedRoster() {
        var teams = new ArrayList<>(List.of("a", "b"));
        var context = new BattlezoneMatchContext(false, true, teams);
        teams.clear();
        assertTrue(rule.evaluate(context).isEmpty());
        assertEquals(List.of("a", "b"), context.livingTeams());
    }
}
