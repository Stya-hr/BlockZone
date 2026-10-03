package dev.stya.blockzone.game.battlezone;

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
        PACKETS.registerPacket(BattlezoneZoneStateS2CPacket.class, NetworkDirection.PLAY_TO_CLIENT);
        PACKETS.registerPacket(BattlezoneBoundaryPreviewS2CPacket.class, NetworkDirection.PLAY_TO_CLIENT);
        PACKETS.registerPacket(BattlezoneFlightStateS2CPacket.class, NetworkDirection.PLAY_TO_CLIENT);
        PACKETS.registerPacket(BattlezoneReleaseC2SPacket.class, NetworkDirection.PLAY_TO_SERVER);
        PACKETS.registerPacket(BattlezoneToggleParachuteC2SPacket.class, NetworkDirection.PLAY_TO_SERVER);
    }

    public static void toggleParachute() {
        PACKETS.getChannel().sendToServer(new BattlezoneToggleParachuteC2SPacket());
    }

    public static void releaseDeployment() {
        PACKETS.getChannel().sendToServer(new BattlezoneReleaseC2SPacket());
    }

    public static void send(ServerPlayer player, BattlezoneFlightStateS2CPacket packet) {
        PACKETS.getChannel().send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void send(ServerPlayer player, BattlezoneZoneStateS2CPacket packet) {
        PACKETS.getChannel().send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void send(ServerPlayer player, BattlezoneBoundaryPreviewS2CPacket packet) {
        PACKETS.getChannel().send(PacketDistributor.PLAYER.with(() -> player), packet);
    }
}
