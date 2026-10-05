package dev.stya.blockzone.net.editor;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import java.util.UUID;
import java.util.function.Supplier;

public record LootEditorResultS2CPacket(UUID token, boolean saved, String messageKey) {
    public static void encode(LootEditorResultS2CPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUUID(packet.token); buffer.writeBoolean(packet.saved); buffer.writeUtf(packet.messageKey, 256);
    }
    public static LootEditorResultS2CPacket decode(FriendlyByteBuf buffer) {
        return new LootEditorResultS2CPacket(buffer.readUUID(), buffer.readBoolean(), buffer.readUtf(256));
    }
    public void handle(Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> dev.stya.blockzone.client.editor.LootCrateEditorScreen.result(this)));
        context.setPacketHandled(true);
    }
}
