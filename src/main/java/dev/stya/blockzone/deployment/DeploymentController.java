package dev.stya.blockzone.deployment;

import dev.stya.blockzone.map.battlezone.BattlezoneMap;
import dev.stya.blockzone.net.battlezone.BattlezoneNetwork;
import dev.stya.blockzone.net.battlezone.DeploymentVehicleS2CPacket;
import dev.stya.blockzone.net.battlezone.FlightStateS2CPacket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.phys.Vec3;

/** Server-owned transport and first-landing protection, scoped to one match. */
public final class DeploymentController {
    private final BattlezoneMap map;
    private final Map<UUID, Flight> flights = new HashMap<>();
    private FlightRoute route;
    private long ticks;

    public DeploymentController(BattlezoneMap map) {
        this.map = map;
    }

    public void start(FlightRoute selected) {
        clear();
        route = selected;
        ticks = 0;
        map.getMapTeams().getNormalTeams().forEach(team -> team.getOnline().forEach(this::board));
    }

    public void board(ServerPlayer player) {
        if (route == null || route.progress(ticks) >= 1 || flights.containsKey(player.getUUID())
                || !isParticipant(player)) {
            return;
        }
        player.stopRiding();
        flights.put(player.getUUID(), new Flight(player));
        player.getAbilities().mayfly = true;
        player.getAbilities().flying = true;
        player.onUpdateAbilities();
        player.setNoGravity(true);
        player.setForcedPose(Pose.SWIMMING);
        float routeYaw = routeYaw();
        player.setYRot(routeYaw);
        player.setYHeadRot(routeYaw);
        player.setXRot(0);
        moveAlongRoute(player, false);
        BattlezoneNetwork.send(player, new FlightStateS2CPacket(player.getUUID(), 1, routeYaw));
        player.sendSystemMessage(Component.translatable("blockzone.deployment.boarded"));
    }

    public void tick() {
        if (route == null) {
            return;
        }
        ticks++;
    }

    public boolean routeFinished() {
        return route == null || route.progress(ticks) >= 1;
    }

    public boolean protects(ServerPlayer player) {
        return flights.containsKey(player.getUUID());
    }

    public void tickPlayer(ServerPlayer player) {
        Flight flight = flights.get(player.getUUID());
        if (flight == null) {
            return;
        }
        if (!map.isMatchActive() || !isParticipant(player) || !player.isAlive()) {
            remove(player);
            return;
        }
        player.fallDistance = 0;
        moveAlongRoute(player, true);
        if (routeFinished()) {
            release(player);
        }
    }

    public boolean release(ServerPlayer player) {
        Flight flight = flights.get(player.getUUID());
        if (flight == null || !map.isMatchActive() || !isParticipant(player)) {
            return false;
        }
        remove(player, false);
        map.beginLanding(player);
        return true;
    }

    private void moveAlongRoute(ServerPlayer player, boolean keepClientLook) {
        player.getAbilities().flying = true;
        player.setNoGravity(true);
        player.fallDistance = 0;
        player.setOnGround(false);
        player.setDeltaMovement(Vec3.ZERO);
        double x = route.x(ticks);
        double z = route.z(ticks);
        if (keepClientLook) {
            // Zero relative rotation keeps the client's newest mouse look, even before it reaches the server.
            player.connection.teleport(x, route.startY(), z,
                    player.getYRot(), player.getXRot(), Set.of(RelativeMovement.Y_ROT, RelativeMovement.X_ROT));
        } else {
            player.teleportTo(map.getServerLevel(), x, route.startY(), z,
                    player.getYRot(), player.getXRot());
        }
    }

    public DeploymentVehicleS2CPacket vehicleSnapshot(boolean deploying) {
        return new DeploymentVehicleS2CPacket(map.getServerLevel().dimension().location(),
                deploying ? route : null, ticks);
    }

    private float routeYaw() {
        return (float) Math.toDegrees(Math.atan2(-(route.endX() - route.startX()), route.endZ() - route.startZ()));
    }

    private boolean isParticipant(ServerPlayer player) {
        return player.serverLevel() == map.getServerLevel() && !player.isSpectator()
                && map.getMapTeams().getTeamByPlayer(player)
                .filter(team -> !team.isSpectator())
                .flatMap(team -> team.getPlayerData(player.getUUID()))
                .map(data -> data.isLiving()).orElse(false);
    }

    public void remove(ServerPlayer player) {
        remove(player, true);
    }

    private void remove(ServerPlayer player, boolean notifyClient) {
        Flight flight = flights.remove(player.getUUID());
        if (flight == null) {
            return;
        }
        restoreFlightAbilities(player, flight);
        player.setForcedPose(flight.pose);
        player.fallDistance = 0;
        player.setDeltaMovement(Vec3.ZERO);
        if (notifyClient) {
            BattlezoneNetwork.send(player, new FlightStateS2CPacket(player.getUUID(), 0));
        }
    }

    private static void restoreFlightAbilities(ServerPlayer player, Flight flight) {
        if (player.gameMode.getGameModeForPlayer() == flight.gameMode) {
            player.getAbilities().mayfly = flight.mayfly;
            player.getAbilities().flying = flight.flying;
        }
        player.setNoGravity(flight.noGravity);
        player.onUpdateAbilities();
    }

    public void clear() {
        for (Flight flight : List.copyOf(flights.values())) {
            remove(flight.player);
        }
        route = null;
        ticks = 0;
    }

    private static final class Flight {
        final ServerPlayer player;
        final boolean mayfly;
        final boolean flying;
        final boolean noGravity;
        final Pose pose;
        final net.minecraft.world.level.GameType gameMode;

        Flight(ServerPlayer player) {
            this.player = player;
            mayfly = player.getAbilities().mayfly;
            flying = player.getAbilities().flying;
            noGravity = player.isNoGravity();
            pose = player.getForcedPose();
            gameMode = player.gameMode.getGameModeForPlayer();
        }
    }
}
