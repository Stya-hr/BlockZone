package dev.stya.blockzone.net.battlezone;

import dev.stya.blockzone.util.battlezone.ZoneGeometry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;
import java.util.List;

public record ZonePreviewS2CPacket(ResourceLocation dimension, boolean visible, List<ZoneGeometry> zones) {
    public ZonePreviewS2CPacket { zones = List.copyOf(zones); }
    public static void encode(ZonePreviewS2CPacket packet, FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(packet.dimension());
        buffer.writeBoolean(packet.visible());
        buffer.writeCollection(packet.zones(), (out, zone) -> {
            out.writeDouble(zone.centerX());
            out.writeDouble(zone.centerY());
            out.writeDouble(zone.centerZ());
            out.writeDouble(zone.radius());
        });
    }
    public static ZonePreviewS2CPacket decode(FriendlyByteBuf buffer) {
        return new ZonePreviewS2CPacket(buffer.readResourceLocation(), buffer.readBoolean(),
                buffer.readList(in -> new ZoneGeometry(in.readDouble(), in.readDouble(), in.readDouble(), in.readDouble())));
    }
    public void handle(Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.enqueueWork(() -> ClientPacketHandler.handle(this));
        context.setPacketHandled(true);
    }
}
