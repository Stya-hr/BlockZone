package dev.stya.blockzone.net.battlezone;

import dev.stya.blockzone.combat.DownedController;
import dev.stya.blockzone.combat.MatchRegeneration;
import dev.stya.blockzone.inventory.InventorySlot;
import dev.stya.blockzone.inventory.PlayerPanelInventory;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

public record DiscardInventoryC2SPacket(int slot, int action, UUID round, ItemStack expectedItem) {
    public static void encode(DiscardInventoryC2SPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.slot); buffer.writeByte(packet.action); buffer.writeUUID(packet.round); buffer.writeItem(packet.expectedItem);
    }
    public static DiscardInventoryC2SPacket decode(FriendlyByteBuf buffer) {
        return new DiscardInventoryC2SPacket(buffer.readVarInt(), buffer.readByte(), buffer.readUUID(), buffer.readItem());
    }
    public void handle(Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.enqueueWork(() -> {
            var player = context.getSender();
            if (player == null || !player.isAlive() || player.isSpectator() || DownedController.busy(player)
                    || MatchRegeneration.allowed(player) || slot < 0 || slot >= InventorySlot.ALL.size()) return;
            PlayerPanelInventory.get(player).filter(PlayerPanelInventory::active).ifPresent(data -> {
                if (MatchRegeneration.map(player).filter(data::matches).isPresent())
                    data.discard(InventorySlot.byIndex(slot), action, round, expectedItem);
            });
        });
        context.setPacketHandled(true);
    }
}
