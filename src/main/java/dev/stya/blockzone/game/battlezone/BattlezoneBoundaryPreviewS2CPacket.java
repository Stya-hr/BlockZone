package dev.stya.blockzone.game.battlezone;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record BattlezoneBoundaryPreviewS2CPacket(ResourceLocation dimension, boolean visible,
                                                  BlockPos pos1, BlockPos pos2) {
    public static void encode(BattlezoneBoundaryPreviewS2CPacket packet, FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(packet.dimension);
        buffer.writeBoolean(packet.visible);
        buffer.writeBlockPos(packet.pos1);
        buffer.writeBlockPos(packet.pos2);
    }

    public static BattlezoneBoundaryPreviewS2CPacket decode(FriendlyByteBuf buffer) {
        return new BattlezoneBoundaryPreviewS2CPacket(buffer.readResourceLocation(), buffer.readBoolean(),
                buffer.readBlockPos(), buffer.readBlockPos());
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> BattlezoneClientPacketHandler.handle(this));
        context.setPacketHandled(true);
    }
}
