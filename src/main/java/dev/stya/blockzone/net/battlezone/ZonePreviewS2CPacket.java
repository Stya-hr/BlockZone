package dev.stya.blockzone.net.battlezone;

import dev.stya.blockzone.util.battlezone.ZoneGeometry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public record ZonePreviewS2CPacket(ResourceLocation dimension, boolean visible, ZoneGeometry zone) {
    public static void encode(ZonePreviewS2CPacket packet, FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(packet.dimension());
        buffer.writeBoolean(packet.visible());
        buffer.writeDouble(packet.zone().centerX());
        buffer.writeDouble(packet.zone().centerY());
        buffer.writeDouble(packet.zone().centerZ());
        buffer.writeDouble(packet.zone().radius());
    }
    public static ZonePreviewS2CPacket decode(FriendlyByteBuf buffer) {
        return new ZonePreviewS2CPacket(buffer.readResourceLocation(), buffer.readBoolean(),
                new ZoneGeometry(buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble()));
    }
    public void handle(Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.enqueueWork(() -> ClientPacketHandler.handle(this));
        context.setPacketHandled(true);
    }
}
