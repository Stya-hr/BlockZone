package dev.stya.blockzone.game.battlezone;

import com.ptcrys.fpsmatch.common.packet.register.NetworkPacketRegister;
import dev.stya.blockzone.BlockZone;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.PacketDistributor;

public final class BattlezoneNetwork {
    private static final NetworkPacketRegister PACKETS = new NetworkPacketRegister(
            ResourceLocation.fromNamespaceAndPath(BlockZone.MOD_ID, "main"), "2");

    private BattlezoneNetwork() {
    }

    public static void register() {
        PACKETS.registerPacket(BattlezoneZoneStateS2CPacket.class, NetworkDirection.PLAY_TO_CLIENT);
        PACKETS.registerPacket(BattlezoneBoundaryPreviewS2CPacket.class, NetworkDirection.PLAY_TO_CLIENT);
    }

    public static void send(ServerPlayer player, BattlezoneZoneStateS2CPacket packet) {
        PACKETS.getChannel().send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void send(ServerPlayer player, BattlezoneBoundaryPreviewS2CPacket packet) {
        PACKETS.getChannel().send(PacketDistributor.PLAYER.with(() -> player), packet);
    }
}
