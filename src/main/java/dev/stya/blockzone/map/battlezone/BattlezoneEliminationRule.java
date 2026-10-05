package dev.stya.blockzone.map.battlezone;

import com.ptcrys.fpsmatch.core.match.RoundResult;
import java.util.List;
import java.util.Optional;

/** Single-match survival rule, evaluated by FPSMatch's BaseMap victory hook. */
final class BattlezoneEliminationRule {
    Optional<RoundResult<String, BattlezoneResultReason>> evaluate(BattlezoneMatchContext context) {
        if (context.debug() || !context.combatPhase()) return Optional.empty();
        return resolve(context.livingTeams());
    }

    Optional<RoundResult<String, BattlezoneResultReason>> resolve(List<String> livingTeams) {
        if (livingTeams.isEmpty()) return Optional.of(new RoundResult<>(null, BattlezoneResultReason.DRAW));
        if (livingTeams.size() == 1)
            return Optional.of(new RoundResult<>(livingTeams.get(0), BattlezoneResultReason.LAST_TEAM_STANDING));
        return Optional.empty();
    }
}
