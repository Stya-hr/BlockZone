package dev.stya.blockzone.net.battlezone;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;
import java.util.UUID;

/** 0 = normal, 1 = on route, 2 = parachute open, 3 = freefall. */
public record FlightStateS2CPacket(UUID playerId, int state, float routeYaw, int appearance) {
    public FlightStateS2CPacket(UUID playerId, int state) {
        this(playerId, state, 0, 0);
    }
    public FlightStateS2CPacket(UUID playerId, int state, float routeYaw) {
        this(playerId, state, routeYaw, 0);
    }

    public static void encode(FlightStateS2CPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUUID(packet.playerId);
        buffer.writeByte(packet.state);
        buffer.writeFloat(packet.routeYaw);
        buffer.writeVarInt(packet.appearance);
    }

    public static FlightStateS2CPacket decode(FriendlyByteBuf buffer) {
        return new FlightStateS2CPacket(buffer.readUUID(), buffer.readUnsignedByte(), buffer.readFloat(), buffer.readVarInt());
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> ClientPacketHandler.handle(this));
        context.setPacketHandled(true);
    }
}
