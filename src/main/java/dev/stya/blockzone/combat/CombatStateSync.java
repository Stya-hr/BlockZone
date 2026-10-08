package dev.stya.blockzone.combat;

import dev.stya.blockzone.net.BlockzoneNetwork;
import dev.stya.blockzone.net.battlezone.*;
import net.minecraft.server.level.ServerPlayer;
import java.util.Map;
import java.util.WeakHashMap;

/** Server-owned presentation cache for combat and teammate rescue. */
public final class CombatStateSync {
    private static final Map<ServerPlayer, CombatStateS2CPacket> COMBAT_STATES = new WeakHashMap<>();
    private CombatStateSync() {}

    public static void syncCombat(ServerPlayer player, boolean active, float platePoints, int plateSlots) {
        var state = new CombatStateS2CPacket(active, platePoints, plateSlots);
        if (java.util.Objects.equals(COMBAT_STATES.get(player), state)) return;
        COMBAT_STATES.put(player, state);
        BlockzoneNetwork.sendToPlayer(player, state);
    }

    private static final Map<ServerPlayer, RescueStateS2CPacket> RESCUE_STATES = new WeakHashMap<>();
    public static RescueStateS2CPacket rescuePacket(ServerPlayer player) {
        return RESCUE_STATES.getOrDefault(player, new RescueStateS2CPacket(player.getUUID(), 0, 0, 0));
    }
    public static void sendRescue(ServerPlayer player, RescueStateS2CPacket packet) {
        var previous = RESCUE_STATES.get(player);
        if (packet.equals(previous)) return;
        if (packet.state() == 0) RESCUE_STATES.remove(player); else RESCUE_STATES.put(player, packet);
        BlockzoneNetwork.sendToTrackingAndSelf(player, packet);
    }
}
