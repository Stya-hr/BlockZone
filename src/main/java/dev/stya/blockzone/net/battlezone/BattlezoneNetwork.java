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
            ResourceLocation.fromNamespaceAndPath(BlockZone.MOD_ID, "main"), "15");

    private static final Map<ServerPlayer, FlightStateS2CPacket> FLIGHT_STATES = new WeakHashMap<>();
    private static final Map<ServerPlayer, CombatStateS2CPacket> COMBAT_STATES = new WeakHashMap<>();

    public static void syncCombat(ServerPlayer player, boolean active, float platePoints) {
        var state = new CombatStateS2CPacket(active, platePoints);
        if (java.util.Objects.equals(COMBAT_STATES.get(player), state)) return;
        COMBAT_STATES.put(player, state);
        PACKETS.getChannel().send(PacketDistributor.PLAYER.with(() -> player), state);
    }

    private BattlezoneNetwork() {
    }

    public static void register() {
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
    }

    public static void sendEditor(ServerPlayer player, Object packet) {
        PACKETS.getChannel().send(PacketDistributor.PLAYER.with(() -> player), packet);
    }
    public static void saveEditor(dev.stya.blockzone.net.editor.SavePoisonEditorC2SPacket packet) {
        PACKETS.getChannel().sendToServer(packet);
    }

    public static void saveLootEditor(dev.stya.blockzone.net.editor.SaveLootEditorC2SPacket packet) {
        PACKETS.getChannel().sendToServer(packet);
    }

    public static void toggleParachute() {
        PACKETS.getChannel().sendToServer(new ToggleParachuteC2SPacket());
    }

    public static void releaseDeployment() {
        PACKETS.getChannel().sendToServer(new ReleaseDeploymentC2SPacket());
    }

    public static void send(ServerPlayer player, FlightStateS2CPacket packet) {
        if (packet.state() == 0) FLIGHT_STATES.remove(player);
        else FLIGHT_STATES.put(player, packet);
        PACKETS.getChannel().send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player), packet);
    }

    public static int flightState(ServerPlayer player) {
        var packet = FLIGHT_STATES.get(player);
        return packet == null ? 0 : packet.state();
    }

    public static FlightStateS2CPacket flightPacket(ServerPlayer player) {
        return FLIGHT_STATES.getOrDefault(player, new FlightStateS2CPacket(player.getUUID(), 0));
    }

    public static void sendFlightStateTo(ServerPlayer observer, FlightStateS2CPacket packet) {
        PACKETS.getChannel().send(PacketDistributor.PLAYER.with(() -> observer), packet);
    }

    public static void send(ServerPlayer player, DeploymentVehicleS2CPacket packet) {
        PACKETS.getChannel().send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void send(ServerPlayer player, ZoneStateS2CPacket packet) {
        PACKETS.getChannel().send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void send(ServerPlayer player, ZonePreviewS2CPacket packet) {
        PACKETS.getChannel().send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void send(ServerPlayer player, BoundaryPreviewS2CPacket packet) {
        PACKETS.getChannel().send(PacketDistributor.PLAYER.with(() -> player), packet);
    }
}
