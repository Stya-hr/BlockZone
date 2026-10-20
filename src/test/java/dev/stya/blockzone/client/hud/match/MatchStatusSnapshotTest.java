package dev.stya.blockzone.client.hud.match;

import com.ptcrys.fpsmatch.core.data.PlayerData;
import com.ptcrys.fpsmatch.core.team.ClientTeam;
import com.ptcrys.fpsmatch.core.team.TeamData;
import java.util.List;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MatchStatusSnapshotTest {
    @Test void countsFrameworkSurvivorsAndKeepsEliminatedPlayersKills() {
        var local = player(false, 3);
        var teammate = player(true, 0);
        var opponent = player(true, 1);
        var first = team("battlezone", "match", "first", local, teammate);
        var second = team("battlezone", "match", "second", opponent);
        var empty = team("battlezone", "match", "empty");
        var eliminated = team("battlezone", "match", "eliminated", player(false, 1));
        var roster = List.of(first, second, empty, eliminated);
        assertEquals(new MatchStatusSnapshot(2, 2, 3, true), MatchStatusSnapshot.capture(roster, "match", local.getOwner()));
        opponent.setLiving(false);
        assertEquals(new MatchStatusSnapshot(1, 1, 3, true), MatchStatusSnapshot.capture(roster, "match", local.getOwner()));
        teammate.setLiving(false);
        assertEquals(new MatchStatusSnapshot(0, 0, 3, true), MatchStatusSnapshot.capture(roster, "match", local.getOwner()));
    }

    @Test void ignoresSpectatorsOtherMapsAndOtherGameTypes() {
        var local = player(true, 99);
        var spectator = team("battlezone", "match", "spectator", local);
        spectator.setSpectator(true);
        var roster = List.of(spectator,
                team("battlezone", "other", "other_map", player(true, 0)),
                team("cs", "match", "other_game", player(true, 0)));
        assertEquals(new MatchStatusSnapshot(0, 0, 0, false), MatchStatusSnapshot.capture(roster, "match", local.getOwner()));
        assertEquals(new MatchStatusSnapshot(0, 0, 0, false), MatchStatusSnapshot.capture(List.of(), "match", local.getOwner()));
    }

    private static PlayerData player(boolean living, int kills) {
        var player = new PlayerData(UUID.randomUUID(), Component.literal("test"));
        player.setLiving(living);
        player.setKills(kills);
        return player;
    }
    private static ClientTeam team(String game, String map, String name, PlayerData... players) {
        var team = new ClientTeam(game, map, new TeamData(name, 4, List.of()));
        for (var player : players) team.setPlayerData(player.getOwner(), player);
        return team;
    }
}
