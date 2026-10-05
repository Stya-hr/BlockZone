package dev.stya.blockzone.net.battlezone;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public record DetachArmorC2SPacket() {
    public static void encode(DetachArmorC2SPacket packet, FriendlyByteBuf buffer) { }
    public static DetachArmorC2SPacket decode(FriendlyByteBuf buffer) { return new DetachArmorC2SPacket(); }
    public void handle(Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.enqueueWork(() -> {
            if (context.getSender() != null) dev.stya.blockzone.equipment.ArmorExpansionItem.detach(context.getSender());
        });
        context.setPacketHandled(true);
    }
}
