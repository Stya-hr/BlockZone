package dev.stya.blockzone.map.battlezone;

import dev.stya.blockzone.deployment.DeploymentController;
import dev.stya.blockzone.net.battlezone.BattlezoneNetwork;
import dev.stya.blockzone.net.battlezone.ZoneStateS2CPacket;
import dev.stya.blockzone.zone.PoisonZoneController;
import java.util.function.Supplier;
import net.minecraft.server.level.ServerPlayer;

/** Owns presentation packets and their cadence; the map owns lifecycle transitions. */
final class BattlezoneVisualSync {
    private static final int SYNC_INTERVAL_TICKS = 5;
    private final BattlezoneMap map;
    private final DeploymentController deployment;
    private final PoisonZoneController poison;
    private final Supplier<String> boundaryTexture;
    private long lastSync = Long.MIN_VALUE;

    BattlezoneVisualSync(BattlezoneMap map, DeploymentController deployment, PoisonZoneController poison,
                         Supplier<String> boundaryTexture) {
        this.map = map;
        this.deployment = deployment;
        this.poison = poison;
        this.boundaryTexture = boundaryTexture;
    }

    void sync(boolean force) {
        if (!force && !map.isStart()) return;
        long gameTime = map.getServerLevel().getGameTime();
        if (!shouldSync(gameTime, lastSync, force)) return;
        lastSync = gameTime;
        var vehicle = deployment.vehicleSnapshot(map.isStart() && map.getPhase() == BattlezoneMap.MatchPhase.DEPLOYMENT);
        for (var player : map.getMapTeams().getOnlineWithSpec()) {
            send(player);
            BattlezoneNetwork.send(player, vehicle);
        }
    }

    static boolean shouldSync(long gameTime, long lastSync, boolean force) {
        return force || (lastSync != Long.MIN_VALUE && gameTime - lastSync >= SYNC_INTERVAL_TICKS);
    }

    void send(ServerPlayer player) { send(player, map.isMatchActive()); }
    void send(ServerPlayer player, boolean boundaryVisible) { BattlezoneNetwork.send(player, packet(player, boundaryVisible)); }

    private ZoneStateS2CPacket packet(ServerPlayer player, boolean boundaryVisible) {
        var area = map.getMapArea();
        boolean participant = player.serverLevel() == map.getServerLevel() && !player.isSpectator()
                && map.getMapTeams().getTeamByPlayer(player).filter(team -> !team.isSpectator())
                .flatMap(team -> team.getPlayerData(player.getUUID())).map(data -> data.isLiving()).orElse(false);
        var zone = poison.current();
        boolean combatPhase = map.getPhase() == BattlezoneMap.MatchPhase.DEPLOYMENT || map.getPhase() == BattlezoneMap.MatchPhase.MATCH;
        return new ZoneStateS2CPacket(map.getMapName(), map.getServerLevel().dimension().location(), boundaryVisible,
                boundaryVisible && participant && combatPhase, area.pos1(), area.pos2(), zone.centerX(), zone.centerZ(),
                (float) zone.radius(), boundaryTexture.get(), zone.shape());
    }
}
