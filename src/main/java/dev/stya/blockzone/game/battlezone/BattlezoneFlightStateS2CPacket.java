package dev.stya.blockzone.game.battlezone;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

/** 0 = normal, 1 = on route, 2 = parachute open, 3 = freefall. */
public record BattlezoneFlightStateS2CPacket(int state) {
    public static void encode(BattlezoneFlightStateS2CPacket packet, FriendlyByteBuf buffer) {
        buffer.writeByte(packet.state);
    }

    public static BattlezoneFlightStateS2CPacket decode(FriendlyByteBuf buffer) {
        return new BattlezoneFlightStateS2CPacket(buffer.readUnsignedByte());
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> BattlezoneClientPacketHandler.handle(this));
        context.setPacketHandled(true);
    }
}
