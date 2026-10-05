package dev.stya.blockzone.net.battlezone;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

/** Authoritative HUD scope; world previews must never enable the match HUD. */
public record CombatStateS2CPacket(boolean active) {
    public static void encode(CombatStateS2CPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.active);
    }
    public static CombatStateS2CPacket decode(FriendlyByteBuf buffer) {
        return new CombatStateS2CPacket(buffer.readBoolean());
    }
    public void handle(Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.enqueueWork(() -> ClientPacketHandler.handle(this));
        context.setPacketHandled(true);
    }
}
