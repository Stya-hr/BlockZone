package dev.stya.blockzone.map.battlezone;

import com.ptcrys.fpsmatch.core.team.ServerTeam;
import com.ptcrys.fpsmatch.core.team.TeamData;
import java.util.List;

/** Reconciles configured squad capacity with the FPSMatch team roster. */
final class BattlezoneTeamRoster {
    private final BattlezoneMap map;

    BattlezoneTeamRoster(BattlezoneMap map) { this.map = map; }

    void reconcile() {
        // Keep the roster available before room selection and reconcile loaded settings.
        int maxPlayers = Math.max(1, map.totalPlayerLimit());
        int playersPerTeam = Math.max(1, map.teamPlayerLimit());
        int capacityTeamCount = (int) (((long) maxPlayers + playersPerTeam - 1) / playersPerTeam);
        int requiredTeams = Math.max(Math.max(1, map.minimumTeamsToStart()), capacityTeamCount);
        List<ServerTeam> teams = map.getMapTeams().getNormalTeams();
        for (ServerTeam team : List.copyOf(teams)) {
            if (!map.isStart() && team.isEmpty() && team.getPlayerLimit() != playersPerTeam) {
                var capabilities = map.getMapTeams().getData();
                map.getMapTeams().delTeam(team.getPlayerTeam());
                map.addTeam(new TeamData(team.getName(), playersPerTeam));
                map.getMapTeams().writeData(capabilities);
            }
        }
        teams = map.getMapTeams().getNormalTeams();
        if (!map.isStart()) {
            for (ServerTeam team : List.copyOf(teams)) {
                if (teams.size() <= requiredTeams) {
                    break;
                }
                if (team.isEmpty() && team.getName().startsWith("squad_")) {
                    map.getMapTeams().delTeam(team.getPlayerTeam());
                    teams = map.getMapTeams().getNormalTeams();
                }
            }
        }
        if (teams.size() >= requiredTeams) {
            return;
        }

        int nextNumber = 1;
        while (teams.size() < requiredTeams) {
            String teamName = "squad_" + nextNumber++;
            if (map.getMapTeams().getTeamByName(teamName).isPresent()) {
                continue;
            }
            map.addTeam(new TeamData(teamName, playersPerTeam));
            teams = map.getMapTeams().getNormalTeams();
        }
    }

}
