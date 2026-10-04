package dev.stya.blockzone.net.editor;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.api.distmarker.Dist;
import java.util.UUID;
import java.util.function.Supplier;

public record PoisonEditorResultS2CPacket(UUID token, boolean saved, String message) {
    public static void encode(PoisonEditorResultS2CPacket p, FriendlyByteBuf b) {
        b.writeUUID(p.token); b.writeBoolean(p.saved); b.writeUtf(p.message);
    }
    public static PoisonEditorResultS2CPacket decode(FriendlyByteBuf b) {
        return new PoisonEditorResultS2CPacket(b.readUUID(), b.readBoolean(), b.readUtf());
    }
    public void handle(Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> dev.stya.blockzone.client.editor.PoisonWorldEditor.result(this)));
        context.setPacketHandled(true);
    }
}
