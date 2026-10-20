package dev.stya.blockzone.client.hud.match;

import com.ptcrys.fpsmatch.core.team.BaseTeam;
import dev.stya.blockzone.map.battlezone.BattlezoneMap;
import java.util.Collection;
import java.util.UUID;

/** A view of FPSMatch accounting, including downed and retained offline participants. */
public record MatchStatusSnapshot(int teams, int players, int kills, boolean participant) {
    public static MatchStatusSnapshot capture(Collection<? extends BaseTeam> roster, String map, UUID localId) {
        int teams = 0, players = 0, kills = 0;
        boolean participant = false;
        for (var team : roster) {
            if (team.isSpectator() || !BattlezoneMap.GAME_TYPE.equals(team.gameType) || !map.equals(team.mapName)) continue;
            int living = 0;
            for (var player : team.getPlayersData()) {
                if (player.isLiving()) living++;
                if (player.getOwner().equals(localId)) {
                    participant = true;
                    kills = Math.max(0, player.getKills());
                }
            }
            players += living;
            if (living > 0) teams++;
        }
        return new MatchStatusSnapshot(teams, players, kills, participant);
    }
}
