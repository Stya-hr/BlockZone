package dev.stya.blockzone.net.editor;

import dev.stya.blockzone.util.editor.LootCrateEdit;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public record SaveLootEditorC2SPacket(UUID token, List<LootCrateEdit> changes) {
    public static void encode(SaveLootEditorC2SPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUUID(packet.token); LootEditorS2CPacket.writeCrates(buffer, packet.changes);
    }
    public static SaveLootEditorC2SPacket decode(FriendlyByteBuf buffer) {
        return new SaveLootEditorC2SPacket(buffer.readUUID(), LootEditorS2CPacket.readCrates(buffer));
    }
    public void handle(Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.enqueueWork(() -> LootEditorSessions.save(context.getSender(), this));
        context.setPacketHandled(true);
    }
}
