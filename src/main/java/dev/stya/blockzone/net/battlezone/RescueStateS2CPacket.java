package dev.stya.blockzone.net.battlezone;

import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public record RescueStateS2CPacket(UUID playerId, int state, int bleedTicks, int rescueTicks, int bleedTotalTicks, int rescueTotalTicks, double rescueDistance) {
    public static void encode(RescueStateS2CPacket p, FriendlyByteBuf b) {
        b.writeUUID(p.playerId); b.writeByte(p.state); b.writeVarInt(p.bleedTicks); b.writeVarInt(p.rescueTicks);
        b.writeVarInt(p.bleedTotalTicks); b.writeVarInt(p.rescueTotalTicks); b.writeDouble(p.rescueDistance);
    }
    public static RescueStateS2CPacket decode(FriendlyByteBuf b) {
        return new RescueStateS2CPacket(b.readUUID(), b.readUnsignedByte(), b.readVarInt(), b.readVarInt(), b.readVarInt(), b.readVarInt(), b.readDouble());
    }
    public void handle(Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.enqueueWork(() -> BattlezoneClientPacketHandler.handle(this));
        context.setPacketHandled(true);
    }
}
