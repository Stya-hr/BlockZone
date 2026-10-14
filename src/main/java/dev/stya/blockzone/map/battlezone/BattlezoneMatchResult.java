package dev.stya.blockzone.map.battlezone;

import com.ptcrys.fpsmatch.common.event.FPSMapEvent;
import java.util.Map;
import java.util.UUID;

/** Read-only framework score snapshots retained through settlement and reset until the next match. */
public record BattlezoneMatchResult(String mapName, String winner, BattlezoneResultReason reason, long elapsedTicks,
        Map<UUID, FPSMapEvent.PlayerScoreSnapshot> players, Map<String, FPSMapEvent.TeamScoreSummary> teams) {
    public BattlezoneMatchResult {
        players = Map.copyOf(players);
        teams = Map.copyOf(teams);
    }
}
