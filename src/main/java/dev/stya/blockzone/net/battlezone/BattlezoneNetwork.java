package dev.stya.blockzone.net.battlezone;

import com.ptcrys.fpsmatch.common.packet.register.NetworkPacketRegister;
import dev.stya.blockzone.BlockZone;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.PacketDistributor;

public final class BattlezoneNetwork {
    private static final NetworkPacketRegister PACKETS = new NetworkPacketRegister(
            ResourceLocation.fromNamespaceAndPath(BlockZone.MOD_ID, "main"), "4");

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
        PACKETS.getChannel().send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void send(ServerPlayer player, ZoneStateS2CPacket packet) {
        PACKETS.getChannel().send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void send(ServerPlayer player, BoundaryPreviewS2CPacket packet) {
        PACKETS.getChannel().send(PacketDistributor.PLAYER.with(() -> player), packet);
    }
}
