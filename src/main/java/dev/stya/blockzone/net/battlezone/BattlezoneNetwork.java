package dev.stya.blockzone.net.battlezone;

import com.ptcrys.fpsmatch.common.packet.register.NetworkPacketRegister;
import dev.stya.blockzone.BlockZone;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.PacketDistributor;
import java.util.Map;
import java.util.WeakHashMap;

public final class BattlezoneNetwork {
    private static final NetworkPacketRegister PACKETS = new NetworkPacketRegister(
            ResourceLocation.fromNamespaceAndPath(BlockZone.MOD_ID, "main"), "5");

    private static final Map<ServerPlayer, Integer> FLIGHT_STATES = new WeakHashMap<>();

    private BattlezoneNetwork() {
    }

    public static void register() {
        PACKETS.registerPacket(ZoneStateS2CPacket.class, NetworkDirection.PLAY_TO_CLIENT);
        PACKETS.registerPacket(BoundaryPreviewS2CPacket.class, NetworkDirection.PLAY_TO_CLIENT);
        PACKETS.registerPacket(FlightStateS2CPacket.class, NetworkDirection.PLAY_TO_CLIENT);
        PACKETS.registerPacket(ReleaseDeploymentC2SPacket.class, NetworkDirection.PLAY_TO_SERVER);
        PACKETS.registerPacket(ToggleParachuteC2SPacket.class, NetworkDirection.PLAY_TO_SERVER);
    }

    public static void toggleParachute() {
        PACKETS.getChannel().sendToServer(new ToggleParachuteC2SPacket());
    }

    public static void releaseDeployment() {
        PACKETS.getChannel().sendToServer(new ReleaseDeploymentC2SPacket());
    }

    public static void send(ServerPlayer player, FlightStateS2CPacket packet) {
        if (packet.state() == 0) FLIGHT_STATES.remove(player);
        else FLIGHT_STATES.put(player, packet.state());
        PACKETS.getChannel().send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player), packet);
    }

    public static int flightState(ServerPlayer player) {
        return FLIGHT_STATES.getOrDefault(player, 0);
    }

    public static void sendFlightStateTo(ServerPlayer observer, FlightStateS2CPacket packet) {
        PACKETS.getChannel().send(PacketDistributor.PLAYER.with(() -> observer), packet);
    }

    public static void send(ServerPlayer player, ZoneStateS2CPacket packet) {
        PACKETS.getChannel().send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void send(ServerPlayer player, BoundaryPreviewS2CPacket packet) {
        PACKETS.getChannel().send(PacketDistributor.PLAYER.with(() -> player), packet);
    }
}
