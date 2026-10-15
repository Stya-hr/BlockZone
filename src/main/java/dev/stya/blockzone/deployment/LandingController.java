package dev.stya.blockzone.deployment;

import dev.stya.blockzone.deployment.FlightStateSync;
import dev.stya.blockzone.net.battlezone.FlightStateS2CPacket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.Vec3;

/** Reusable descent and first-landing protection for deployment and future airborne respawns. */
public final class LandingController {
    private final Map<UUID, LandingState> players = new HashMap<>();
    private final ParachuteAppearance appearances = new ParachuteAppearance();

    /** Call after positioning a living player in the air. Repeated calls preserve the original state. */
    public void begin(ServerPlayer player) {
        if (!player.isAlive() || player.isSpectator() || players.containsKey(player.getUUID())) {
            return;
        }
        int appearance = com.ptcrys.fpsmatch.core.FPSMCore.getInstance().getMapByPlayerWithSpec(player)
                .flatMap(map -> map.getMapTeams().getTeamByPlayer(player))
                .map(team -> appearances.forTeam(team.getName()))
                .orElseGet(() -> appearances.forTeam(player.getUUID().toString()));
        players.put(player.getUUID(), new LandingState(player, appearance));
        player.stopRiding();
        player.getAbilities().mayfly = true;
        player.getAbilities().flying = false;
        player.onUpdateAbilities();
        player.setNoGravity(true);
        player.setForcedPose(Pose.SWIMMING);
        player.fallDistance = 0;
        player.setOnGround(false);
        player.setDeltaMovement(0, -0.8, 0);
        player.connection.send(new ClientboundSetEntityMotionPacket(player));
        FlightStateSync.sync(player, new FlightStateS2CPacket(player.getUUID(), 3, 0, appearance));
    }

    public boolean toggleParachute(ServerPlayer player) {
        LandingState state = players.get(player.getUUID());
        if (state == null || !player.isAlive() || player.isSpectator() || player.onGround()) {
            return false;
        }
        long now = player.level().getGameTime();
        if (now - state.lastToggleTick < 5) {
            return false;
        }
        state.lastToggleTick = now;
        state.parachuteOpen = !state.parachuteOpen;
        FlightStateSync.sync(player, new FlightStateS2CPacket(player.getUUID(), state.parachuteOpen ? 2 : 3, 0, state.appearance));
        return true;
    }

    public boolean isDescending(ServerPlayer player) {
        return players.containsKey(player.getUUID());
    }

    public void control(ServerPlayer player, int forward, int left) {
        var state = players.get(player.getUUID());
        if (state == null || !player.isAlive() || player.isSpectator()
                || Math.abs(forward) > 1 || Math.abs(left) > 1) return;
        state.forward = forward;
        state.left = left;
        state.lastInputTick = player.level().getGameTime();
    }

    /** Run at the end of the server player tick, after collision and fall damage callbacks. */
    public void tick(ServerPlayer player) {
        if (!isDescending(player)) {
            return;
        }
        player.fallDistance = 0;
        if (!player.isAlive() || player.isSpectator() || player.onGround()
                || player.isInWater() || player.isInLava() || player.onClimbable()) {
            finish(player);
            return;
        }
        player.getAbilities().flying = false;
        player.setNoGravity(true);
        Vec3 movement = player.getDeltaMovement();
        LandingState state = players.get(player.getUUID());
        if (player.level().getGameTime() - state.lastInputTick > 20) state.forward = state.left = 0;
        var motion = state.parachuteOpen
                ? ParachuteMotion.step(movement.x, movement.y, movement.z, player.getYRot(), player.getXRot(), state.forward, state.left)
                : ParachuteMotion.freefall(movement.x, movement.y, movement.z, player.getYRot(), state.forward, state.left);
        player.setDeltaMovement(motion.x(), motion.y(), motion.z());
        player.connection.send(new ClientboundSetEntityMotionPacket(player));
    }

    /** Ends immunity immediately and restores pre-descent movement state. */
    public void finish(ServerPlayer player) {
        LandingState state = players.remove(player.getUUID());
        if (state == null) {
            return;
        }
        if (player.gameMode.getGameModeForPlayer() == state.gameMode) {
            player.getAbilities().mayfly = state.mayfly;
            player.getAbilities().flying = state.flying;
        }
        player.setNoGravity(state.noGravity);
        player.setForcedPose(state.pose);
        player.onUpdateAbilities();
        player.fallDistance = 0;
        player.setDeltaMovement(Vec3.ZERO);
        FlightStateSync.sync(player, new FlightStateS2CPacket(player.getUUID(), 0));
    }

    public void clear() {
        for (LandingState state : List.copyOf(players.values())) {
            finish(state.player);
        }
        appearances.reset();
    }

    private static final class LandingState {
        final ServerPlayer player;
        final int appearance;
        final boolean mayfly;
        final boolean flying;
        final boolean noGravity;
        boolean parachuteOpen;
        int forward, left;
        long lastInputTick;
        long lastToggleTick = Long.MIN_VALUE / 2;
        final Pose pose;
        final net.minecraft.world.level.GameType gameMode;

        LandingState(ServerPlayer player, int appearance) {
            this.appearance = appearance;
            this.player = player;
            mayfly = player.getAbilities().mayfly;
            flying = player.getAbilities().flying;
            noGravity = player.isNoGravity();
            pose = player.getForcedPose();
            gameMode = player.gameMode.getGameModeForPlayer();
        }
    }
}
