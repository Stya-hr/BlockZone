package dev.stya.blockzone.loot;

import com.ptcrys.fpsmatch.core.FPSMCore;
import dev.stya.blockzone.map.battlezone.BattlezoneMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/** Map ownership is resolved on the server; crates outside arenas work as standalone loot crates. */
public final class LootCrateAccess {
    private LootCrateAccess() { }

    public static List<BattlezoneMap> mapsAt(ServerLevel level, BlockPos pos) {
        return FPSMCore.getInstance().getMapByClass(BattlezoneMap.class).stream()
                .filter(map -> map.getServerLevel() == level)
                .filter(map -> {
                    var area = map.getMapArea();
                    return area != null
                            && pos.getX() >= Math.min(area.pos1().getX(), area.pos2().getX())
                            && pos.getX() <= Math.max(area.pos1().getX(), area.pos2().getX())
                            && pos.getY() >= Math.min(area.pos1().getY(), area.pos2().getY())
                            && pos.getY() <= Math.max(area.pos1().getY(), area.pos2().getY())
                            && pos.getZ() >= Math.min(area.pos1().getZ(), area.pos2().getZ())
                            && pos.getZ() <= Math.max(area.pos1().getZ(), area.pos2().getZ());
                }).toList();
    }

    public static boolean mayEdit(ServerLevel level, BlockPos pos) {
        return mapsAt(level, pos).stream().allMatch(BattlezoneMap::canEditLootCrates);
    }

    public static boolean mayLoot(BattlezoneMap map, ServerPlayer player) {
        return player.serverLevel() == map.getServerLevel() && !player.isSpectator()
                && (map.getPhase() == BattlezoneMap.MatchPhase.DEPLOYMENT
                    || map.getPhase() == BattlezoneMap.MatchPhase.MATCH)
                && map.isStart()
                && map.getMapTeams().getTeamByPlayer(player).filter(team -> !team.isSpectator())
                    .flatMap(team -> team.getPlayerData(player.getUUID())).map(data -> data.isLiving()).orElse(false);
    }
}
