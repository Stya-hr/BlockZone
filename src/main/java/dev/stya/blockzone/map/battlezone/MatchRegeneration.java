package dev.stya.blockzone.map.battlezone;

import com.ptcrys.fpsmatch.core.FPSMCore;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/** Disable vanilla automatic healing for living participants without changing world gamerules. */
public final class MatchRegeneration {
    private MatchRegeneration() { }

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
