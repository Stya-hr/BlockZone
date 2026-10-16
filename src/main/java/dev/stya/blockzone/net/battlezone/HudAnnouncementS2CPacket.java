package dev.stya.blockzone.net.battlezone;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public record HudAnnouncementS2CPacket(String map, String key, Component title, Component detail,
                                       int color, int priority, int durationTicks) {
    public static void encode(HudAnnouncementS2CPacket p, FriendlyByteBuf b) {
        b.writeUtf(p.map, 128); b.writeUtf(p.key, 128); b.writeComponent(p.title); b.writeComponent(p.detail);
        b.writeInt(p.color); b.writeVarInt(p.priority); b.writeVarInt(p.durationTicks);
    }
    public static HudAnnouncementS2CPacket decode(FriendlyByteBuf b) {
        return new HudAnnouncementS2CPacket(b.readUtf(128), b.readUtf(128), b.readComponent(), b.readComponent(), b.readInt(), b.readVarInt(), b.readVarInt());
    }
    public void handle(Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.enqueueWork(() -> BattlezoneClientPacketHandler.handle(this));
        context.setPacketHandled(true);
    }
}
