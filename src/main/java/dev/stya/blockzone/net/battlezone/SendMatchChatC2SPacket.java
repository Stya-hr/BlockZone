package dev.stya.blockzone.net.battlezone;

import dev.stya.blockzone.chat.ChatChannel;
import dev.stya.blockzone.chat.MatchChat;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public record SendMatchChatC2SPacket(ChatChannel channel, String text) {
    public static void encode(SendMatchChatC2SPacket packet, FriendlyByteBuf buffer) {
        buffer.writeEnum(packet.channel); buffer.writeUtf(packet.text, 256);
    }
    public static SendMatchChatC2SPacket decode(FriendlyByteBuf buffer) {
        return new SendMatchChatC2SPacket(buffer.readEnum(ChatChannel.class), buffer.readUtf(256));
    }
    public void handle(Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.enqueueWork(() -> {
            var sender = context.getSender();
            if (sender != null) MatchChat.send(sender, channel, text);
        });
        context.setPacketHandled(true);
    }
}
