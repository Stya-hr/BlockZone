package dev.stya.blockzone.net.battlezone;

import dev.stya.blockzone.map.battlezone.FlightRoute;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

/** A map-owned transport snapshot. A null route removes the vehicle. */
public record DeploymentVehicleS2CPacket(ResourceLocation dimension, FlightRoute route, long elapsedTicks) {
    public static void encode(DeploymentVehicleS2CPacket packet, FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(packet.dimension);
        buffer.writeBoolean(packet.route != null);
        if (packet.route != null) {
            var route = packet.route;
            buffer.writeDouble(route.startX());
            buffer.writeDouble(route.startY());
            buffer.writeDouble(route.startZ());
            buffer.writeDouble(route.endX());
            buffer.writeDouble(route.endY());
            buffer.writeDouble(route.endZ());
            buffer.writeDouble(route.speed());
            buffer.writeVarLong(packet.elapsedTicks);
        }
    }

    public static DeploymentVehicleS2CPacket decode(FriendlyByteBuf buffer) {
        var dimension = buffer.readResourceLocation();
        if (!buffer.readBoolean()) return new DeploymentVehicleS2CPacket(dimension, null, 0);
        var route = new FlightRoute(buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
                buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
        return new DeploymentVehicleS2CPacket(dimension, route, buffer.readVarLong());
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.enqueueWork(() -> ClientPacketHandler.handle(this));
        context.setPacketHandled(true);
    }
}
