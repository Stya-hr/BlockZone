package dev.stya.blockzone.net.battlezone;

import dev.stya.blockzone.chat.ChatChannel;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.UUID;
import java.util.function.Supplier;

public record MatchChatS2CPacket(String map, ChatChannel channel, UUID sender, String name, String text) {
    public static void encode(MatchChatS2CPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.map, 256); buffer.writeEnum(packet.channel); buffer.writeUUID(packet.sender);
        buffer.writeUtf(packet.name, 256); buffer.writeUtf(packet.text, 256);
    }
    public static MatchChatS2CPacket decode(FriendlyByteBuf buffer) {
        return new MatchChatS2CPacket(buffer.readUtf(256), buffer.readEnum(ChatChannel.class), buffer.readUUID(), buffer.readUtf(256), buffer.readUtf(256));
    }
    public void handle(Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.enqueueWork(() -> BattlezoneClientPacketHandler.handle(this));
        context.setPacketHandled(true);
    }
}
