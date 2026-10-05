package dev.stya.blockzone.net.editor;

import dev.stya.blockzone.client.editor.LootCrateEditorScreen;
import dev.stya.blockzone.editor.loot.LootCrateEdit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public record LootEditorS2CPacket(UUID token, String mapName, ResourceLocation dimension, BlockPos pos1,
                                 BlockPos pos2, List<LootCrateEdit> crates, List<String> tables) {
    public static void encode(LootEditorS2CPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUUID(packet.token); buffer.writeUtf(packet.mapName); buffer.writeResourceLocation(packet.dimension);
        buffer.writeBlockPos(packet.pos1); buffer.writeBlockPos(packet.pos2);
        writeCrates(buffer, packet.crates);
        buffer.writeVarInt(packet.tables.size()); packet.tables.forEach(table -> buffer.writeUtf(table, 256));
    }
    public static LootEditorS2CPacket decode(FriendlyByteBuf buffer) {
        var token = buffer.readUUID(); var name = buffer.readUtf(); var dimension = buffer.readResourceLocation();
        var first = buffer.readBlockPos(); var second = buffer.readBlockPos(); var crates = readCrates(buffer);
        int count = boundedCount(buffer, 4096);
        var tables = new ArrayList<String>();
        for (int i = 0; i < count; i++) tables.add(buffer.readUtf(256));
        return new LootEditorS2CPacket(token, name, dimension, first, second, crates, List.copyOf(tables));
    }
    static void writeCrates(FriendlyByteBuf buffer, List<LootCrateEdit> crates) {
        buffer.writeVarInt(crates.size());
        for (var crate : crates) {
            buffer.writeBlockPos(new BlockPos(crate.x(), crate.y(), crate.z()));
            buffer.writeUtf(crate.table(), 256); buffer.writeLong(crate.seed()); buffer.writeBoolean(crate.opened());
            buffer.writeBoolean(crate.enabled()); buffer.writeUtf(crate.block(),256);
        }
    }
    static List<LootCrateEdit> readCrates(FriendlyByteBuf buffer) {
        int count = boundedCount(buffer, LootCrateEdit.MAX_CRATES);
        var crates = new ArrayList<LootCrateEdit>();
        for (int i = 0; i < count; i++) {
            var pos = buffer.readBlockPos();
            crates.add(new LootCrateEdit(pos.getX(), pos.getY(), pos.getZ(), buffer.readUtf(256), buffer.readLong(), buffer.readBoolean(), buffer.readBoolean(), buffer.readUtf(256)));
        }
        return List.copyOf(crates);
    }
    private static int boundedCount(FriendlyByteBuf buffer, int max) {
        int count = buffer.readVarInt();
        if (count < 0 || count > max) throw new IllegalArgumentException("Editor list exceeds limit");
        return count;
    }
    public void handle(Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> LootCrateEditorScreen.open(this)));
        context.setPacketHandled(true);
    }
}
