package dev.stya.blockzone.net.battlezone;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;
import java.util.UUID;

/** 0 = normal, 1 = on route, 2 = parachute open, 3 = freefall. */
public record FlightStateS2CPacket(UUID playerId, int state) {
    public static void encode(FlightStateS2CPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUUID(packet.playerId);
        buffer.writeByte(packet.state);
    }

    public static FlightStateS2CPacket decode(FriendlyByteBuf buffer) {
        return new FlightStateS2CPacket(buffer.readUUID(), buffer.readUnsignedByte());
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> ClientPacketHandler.handle(this));
        context.setPacketHandled(true);
    }
}
