package dev.stya.blockzone.net.battlezone;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public record InventoryStateS2CPacket(CompoundTag state) {
    public static void encode(InventoryStateS2CPacket packet, FriendlyByteBuf buffer) { buffer.writeNbt(packet.state); }
    public static InventoryStateS2CPacket decode(FriendlyByteBuf buffer) {
        return new InventoryStateS2CPacket(java.util.Objects.requireNonNullElseGet(buffer.readNbt(), CompoundTag::new));
    }
    public void handle(Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.enqueueWork(() -> BattlezoneClientPacketHandler.handle(this));
        context.setPacketHandled(true);
    }
}
