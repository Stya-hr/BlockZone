package dev.stya.blockzone.net.editor;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.api.distmarker.Dist;
import java.util.UUID;
import java.util.function.Supplier;

public record PoisonEditorS2CPacket(UUID token, String mapName, ResourceLocation dimension,
        BlockPos pos1, BlockPos pos2, String pathsJson) {
    public static void encode(PoisonEditorS2CPacket p, FriendlyByteBuf b) {
        b.writeUUID(p.token); b.writeUtf(p.mapName); b.writeResourceLocation(p.dimension);
        b.writeBlockPos(p.pos1); b.writeBlockPos(p.pos2); b.writeUtf(p.pathsJson, 262144);
    }
    public static PoisonEditorS2CPacket decode(FriendlyByteBuf b) {
        return new PoisonEditorS2CPacket(b.readUUID(), b.readUtf(), b.readResourceLocation(),
                b.readBlockPos(), b.readBlockPos(), b.readUtf(262144));
    }
    public void handle(Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> dev.stya.blockzone.client.editor.PoisonWorldEditor.open(this)));
        context.setPacketHandled(true);
    }
}
