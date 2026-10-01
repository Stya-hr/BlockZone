package dev.stya.blockzone.game.battlezone;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record BattlezoneZoneStateS2CPacket(
        String mapName,
        ResourceLocation dimension,
        boolean boundaryVisible,
        boolean whiteoutActive,
        BlockPos areaPos1,
        BlockPos areaPos2,
        double centerX,
        double centerZ,
        float radius
) {
    public static void encode(BattlezoneZoneStateS2CPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.mapName, 128);
        buffer.writeResourceLocation(packet.dimension);
        buffer.writeBoolean(packet.boundaryVisible);
        buffer.writeBoolean(packet.whiteoutActive);
        buffer.writeBlockPos(packet.areaPos1);
        buffer.writeBlockPos(packet.areaPos2);
        buffer.writeDouble(packet.centerX);
        buffer.writeDouble(packet.centerZ);
        buffer.writeFloat(packet.radius);
    }

    public static BattlezoneZoneStateS2CPacket decode(FriendlyByteBuf buffer) {
        return new BattlezoneZoneStateS2CPacket(
                buffer.readUtf(128),
                buffer.readResourceLocation(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readBlockPos(),
                buffer.readBlockPos(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readFloat());
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> BattlezoneClientPacketHandler.handle(this));
        context.setPacketHandled(true);
    }
}
