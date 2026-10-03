package dev.stya.blockzone.game.battlezone;

import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Reusable descent and first-landing protection for deployment and future airborne respawns. */
public final class BattlezoneLanding {
    private final Map<UUID, LandingState> players = new HashMap<>();

    /** Call after positioning a living player in the air. Repeated calls preserve the original state. */
    public void begin(ServerPlayer player) {
        if (!player.isAlive() || player.isSpectator() || players.containsKey(player.getUUID())) {
            return;
        }
        players.put(player.getUUID(), new LandingState(player));
        player.stopRiding();
        player.getAbilities().mayfly = false;
        player.getAbilities().flying = false;
        player.onUpdateAbilities();
        player.setNoGravity(false);
        player.setForcedPose(Pose.SWIMMING);
        player.fallDistance = 0;
        player.setOnGround(false);
        player.setXRot(70);
        player.setDeltaMovement(0, -1.5, 0);
        player.connection.send(new ClientboundSetEntityMotionPacket(player));
        BattlezoneNetwork.send(player, new BattlezoneFlightStateS2CPacket(2));
    }

    public boolean isDescending(ServerPlayer player) {
        return players.containsKey(player.getUUID());
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
        player.setNoGravity(false);
        Vec3 movement = player.getDeltaMovement();
        player.setDeltaMovement(movement.x, Math.min(movement.y, -1.5), movement.z);
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
        BattlezoneNetwork.send(player, new BattlezoneFlightStateS2CPacket(0));
    }

    public void clear() {
        for (LandingState state : List.copyOf(players.values())) {
            finish(state.player);
        }
    }

    private static final class LandingState {
        final ServerPlayer player;
        final boolean mayfly;
        final boolean flying;
        final boolean noGravity;
        final Pose pose;
        final net.minecraft.world.level.GameType gameMode;

        LandingState(ServerPlayer player) {
            this.player = player;
            mayfly = player.getAbilities().mayfly;
            flying = player.getAbilities().flying;
            noGravity = player.isNoGravity();
            pose = player.getForcedPose();
            gameMode = player.gameMode.getGameModeForPlayer();
        }
    }
}
