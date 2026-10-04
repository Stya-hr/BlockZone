package dev.stya.blockzone.net.battlezone;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public record ZoneStateS2CPacket(
        String mapName,
        ResourceLocation dimension,
        boolean boundaryVisible,
        boolean whiteoutActive,
        BlockPos areaPos1,
        BlockPos areaPos2,
        double centerX,
        double centerZ,
        float radius,
        String boundaryTexture,
        dev.stya.blockzone.util.battlezone.ZoneShape shape
) {
    public static void encode(ZoneStateS2CPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.mapName, 128);
        buffer.writeResourceLocation(packet.dimension);
        buffer.writeBoolean(packet.boundaryVisible);
        buffer.writeBoolean(packet.whiteoutActive);
        buffer.writeBlockPos(packet.areaPos1);
        buffer.writeBlockPos(packet.areaPos2);
        buffer.writeDouble(packet.centerX);
        buffer.writeDouble(packet.centerZ);
        buffer.writeFloat(packet.radius);
        buffer.writeUtf(packet.boundaryTexture, 256);
        buffer.writeEnum(packet.shape);
    }

    public static ZoneStateS2CPacket decode(FriendlyByteBuf buffer) {
        return new ZoneStateS2CPacket(
                buffer.readUtf(128),
                buffer.readResourceLocation(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readBlockPos(),
                buffer.readBlockPos(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readFloat(),
                buffer.readUtf(256),
                buffer.readEnum(dev.stya.blockzone.util.battlezone.ZoneShape.class));
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> ClientPacketHandler.handle(this));
        context.setPacketHandled(true);
    }
}
