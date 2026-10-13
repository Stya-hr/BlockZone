package dev.stya.blockzone.net.battlezone;

import dev.stya.blockzone.combat.DownedController;
import dev.stya.blockzone.combat.MatchRegeneration;
import dev.stya.blockzone.inventory.PlayerPanelInventory;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.network.NetworkEvent;

public record SwapInventoryC2SPacket(int from, int to, UUID round, int fromRevision, int toRevision) {
    public static void encode(SwapInventoryC2SPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.from); buffer.writeVarInt(packet.to); buffer.writeUUID(packet.round);
        buffer.writeVarInt(packet.fromRevision); buffer.writeVarInt(packet.toRevision);
    }
    public static SwapInventoryC2SPacket decode(FriendlyByteBuf buffer) {
        return new SwapInventoryC2SPacket(buffer.readVarInt(), buffer.readVarInt(), buffer.readUUID(), buffer.readVarInt(), buffer.readVarInt());
    }
    public void handle(Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.enqueueWork(() -> {
            var player = context.getSender();
            if (player == null || !player.isAlive() || player.isSpectator() || DownedController.busy(player)
                    || MatchRegeneration.allowed(player) || from < 0 || to < 0
                    || from >= dev.stya.blockzone.inventory.InventorySlot.ALL.size() || to >= dev.stya.blockzone.inventory.InventorySlot.ALL.size()) return;
            PlayerPanelInventory.get(player).filter(PlayerPanelInventory::active).ifPresent(data -> {
                if (MatchRegeneration.map(player).filter(data::matches).isPresent()
                        && !data.swapSlots(dev.stya.blockzone.inventory.InventorySlot.byIndex(from), dev.stya.blockzone.inventory.InventorySlot.byIndex(to), round, fromRevision, toRevision))
                    player.displayClientMessage(Component.translatable("inventory.blockzone.swap_rejected"), true);
            });
        });
        context.setPacketHandled(true);
    }
}
