package dev.stya.blockzone.deployment;

import dev.stya.blockzone.net.BlockzoneNetwork;
import dev.stya.blockzone.net.battlezone.*;
import net.minecraft.server.level.ServerPlayer;
import java.util.Map;
import java.util.WeakHashMap;

/** Server-owned last flight pose, replayed when an observer starts tracking a player. */
public final class FlightStateSync {
    private static final Map<ServerPlayer, FlightStateS2CPacket> FLIGHT_STATES = new WeakHashMap<>();
    private FlightStateSync() {}

    public static void sync(ServerPlayer player, FlightStateS2CPacket packet) {
        if (packet.state() == 0) FLIGHT_STATES.remove(player);
        else FLIGHT_STATES.put(player, packet);
        BlockzoneNetwork.sendToTrackingAndSelf(player, packet);
    }

    public static int flightState(ServerPlayer player) {
        var packet = FLIGHT_STATES.get(player);
        return packet == null ? 0 : packet.state();
    }

    public static FlightStateS2CPacket flightPacket(ServerPlayer player) {
        return FLIGHT_STATES.getOrDefault(player, new FlightStateS2CPacket(player.getUUID(), 0));
    }

    public static void sendFlightStateTo(ServerPlayer observer, FlightStateS2CPacket packet) {
        BlockzoneNetwork.sendToPlayer(observer, packet);
    }

}
