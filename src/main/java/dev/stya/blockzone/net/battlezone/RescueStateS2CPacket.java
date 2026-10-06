package dev.stya.blockzone.net.battlezone;

import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public record RescueStateS2CPacket(UUID playerId, int state, int bleedTicks, int rescueTicks) {
    public static void encode(RescueStateS2CPacket p, FriendlyByteBuf b) {
        b.writeUUID(p.playerId); b.writeByte(p.state); b.writeVarInt(p.bleedTicks); b.writeVarInt(p.rescueTicks);
    }
    public static RescueStateS2CPacket decode(FriendlyByteBuf b) {
        return new RescueStateS2CPacket(b.readUUID(), b.readUnsignedByte(), b.readVarInt(), b.readVarInt());
    }
    public void handle(Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.enqueueWork(() -> ClientPacketHandler.handle(this));
        context.setPacketHandled(true);
    }
}
