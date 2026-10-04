package dev.stya.blockzone.net.editor;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.UUID;
import java.util.function.Supplier;

public record SavePoisonEditorC2SPacket(UUID token, String pathsJson) {
    public static void encode(SavePoisonEditorC2SPacket p, FriendlyByteBuf b) {
        b.writeUUID(p.token); b.writeUtf(p.pathsJson, 262144);
    }
    public static SavePoisonEditorC2SPacket decode(FriendlyByteBuf b) {
        return new SavePoisonEditorC2SPacket(b.readUUID(), b.readUtf(262144));
    }
    public void handle(Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.enqueueWork(() -> PoisonEditorSessions.save(context.getSender(), this));
        context.setPacketHandled(true);
    }
}
