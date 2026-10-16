package dev.stya.blockzone.net;

import com.ptcrys.fpsmatch.common.packet.register.NetworkPacketRegister;
import dev.stya.blockzone.BlockZone;
import dev.stya.blockzone.net.battlezone.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.PacketDistributor;

/** Shared channel registration and transport. Feature state belongs to feature synchronizers. */
public final class BlockzoneNetwork {
    private static final NetworkPacketRegister PACKETS = new NetworkPacketRegister(
            ResourceLocation.fromNamespaceAndPath(BlockZone.MOD_ID, "main"), "33");

    private BlockzoneNetwork() {}

    public static void register() {
        PACKETS.registerPacket(HudAnnouncementS2CPacket.class, NetworkDirection.PLAY_TO_CLIENT);
        PACKETS.registerPacket(MatchGuidanceS2CPacket.class, NetworkDirection.PLAY_TO_CLIENT);
        PACKETS.registerPacket(SendMatchChatC2SPacket.class, NetworkDirection.PLAY_TO_SERVER);
        PACKETS.registerPacket(MatchChatS2CPacket.class, NetworkDirection.PLAY_TO_CLIENT);
        PACKETS.registerPacket(SquadStateS2CPacket.class, NetworkDirection.PLAY_TO_CLIENT);
        PACKETS.registerPacket(SwapInventoryC2SPacket.class, NetworkDirection.PLAY_TO_SERVER);
        PACKETS.registerPacket(DiscardInventoryC2SPacket.class, NetworkDirection.PLAY_TO_SERVER);
        PACKETS.registerPacket(InventoryStateS2CPacket.class, NetworkDirection.PLAY_TO_CLIENT);
        PACKETS.registerPacket(RescueStateS2CPacket.class, NetworkDirection.PLAY_TO_CLIENT);

        PACKETS.registerPacket(CombatStateS2CPacket.class, NetworkDirection.PLAY_TO_CLIENT);
        PACKETS.registerPacket(dev.stya.blockzone.net.editor.LootEditorS2CPacket.class, NetworkDirection.PLAY_TO_CLIENT);
        PACKETS.registerPacket(dev.stya.blockzone.net.editor.LootEditorResultS2CPacket.class, NetworkDirection.PLAY_TO_CLIENT);
        PACKETS.registerPacket(dev.stya.blockzone.net.editor.SaveLootEditorC2SPacket.class, NetworkDirection.PLAY_TO_SERVER);
        PACKETS.registerPacket(dev.stya.blockzone.net.editor.PoisonEditorS2CPacket.class, NetworkDirection.PLAY_TO_CLIENT);
        PACKETS.registerPacket(dev.stya.blockzone.net.editor.PoisonEditorResultS2CPacket.class, NetworkDirection.PLAY_TO_CLIENT);
        PACKETS.registerPacket(dev.stya.blockzone.net.editor.SavePoisonEditorC2SPacket.class, NetworkDirection.PLAY_TO_SERVER);
        PACKETS.registerPacket(ZonePreviewS2CPacket.class, NetworkDirection.PLAY_TO_CLIENT);
        PACKETS.registerPacket(ZoneStateS2CPacket.class, NetworkDirection.PLAY_TO_CLIENT);
        PACKETS.registerPacket(BoundaryPreviewS2CPacket.class, NetworkDirection.PLAY_TO_CLIENT);
        PACKETS.registerPacket(FlightStateS2CPacket.class, NetworkDirection.PLAY_TO_CLIENT);
        PACKETS.registerPacket(ReleaseDeploymentC2SPacket.class, NetworkDirection.PLAY_TO_SERVER);
        PACKETS.registerPacket(ToggleParachuteC2SPacket.class, NetworkDirection.PLAY_TO_SERVER);
        PACKETS.registerPacket(DeploymentVehicleS2CPacket.class, NetworkDirection.PLAY_TO_CLIENT);
        PACKETS.registerPacket(DescentInputC2SPacket.class, NetworkDirection.PLAY_TO_SERVER);
    }

    public static void sendToPlayer(ServerPlayer player, Object packet) {
        PACKETS.getChannel().send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void sendToTrackingAndSelf(ServerPlayer player, Object packet) {
        PACKETS.getChannel().send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player), packet);
    }

    public static void sendToServer(Object packet) {
        PACKETS.getChannel().sendToServer(packet);
    }
}
