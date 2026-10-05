package dev.stya.blockzone.combat;

import com.ptcrys.fpsmatch.core.FPSMCore;
import dev.stya.blockzone.map.battlezone.BattlezoneMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/** Suppress vanilla healing while the match's recovery clock owns health regeneration. */
public final class MatchRegeneration {
    private MatchRegeneration() { }

    public static java.util.Optional<BattlezoneMap> map(ServerPlayer player) {
        return FPSMCore.getInstance().getMapByPlayerWithSpec(player)
                .filter(BattlezoneMap.class::isInstance).map(BattlezoneMap.class::cast);
    }

    public static boolean allowed(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) return true;
        return FPSMCore.getInstance().getMapByPlayerWithSpec(serverPlayer)
                .filter(BattlezoneMap.class::isInstance).map(BattlezoneMap.class::cast)
                .filter(map -> map.isMatchActive()
                        && (map.getPhase() == BattlezoneMap.MatchPhase.DEPLOYMENT
                        || map.getPhase() == BattlezoneMap.MatchPhase.MATCH)
                        && serverPlayer.serverLevel() == map.getServerLevel()
                        && !serverPlayer.isSpectator())
                .flatMap(map -> map.getMapTeams().getTeamByPlayer(serverPlayer))
                .filter(team -> !team.isSpectator())
                .flatMap(team -> team.getPlayerData(serverPlayer.getUUID()))
                .map(data -> !data.isLiving()).orElse(true);
    }
}
