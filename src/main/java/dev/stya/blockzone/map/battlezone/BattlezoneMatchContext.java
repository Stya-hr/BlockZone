package dev.stya.blockzone.map.battlezone;

import com.ptcrys.fpsmatch.core.match.RoundContext;
import com.ptcrys.fpsmatch.core.team.ServerTeam;
import java.util.List;

/** Immutable rule input; evaluating a result cannot mutate players or map state. */
record BattlezoneMatchContext(boolean debug, boolean combatPhase, List<String> livingTeams) implements RoundContext {
    BattlezoneMatchContext { livingTeams = List.copyOf(livingTeams); }

    static BattlezoneMatchContext capture(BattlezoneMap map) {
        return new BattlezoneMatchContext(map.isDebug(), map.getPhase() == BattlezoneMap.MatchPhase.MATCH,
                map.getMapTeams().getNormalTeams().stream()
                        .filter(team -> !team.getLivingPlayers().isEmpty()).map(ServerTeam::getName).toList());
    }
}
