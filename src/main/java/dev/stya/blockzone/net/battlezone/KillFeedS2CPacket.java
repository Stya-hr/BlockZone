package dev.stya.blockzone.net.battlezone;

import dev.stya.blockzone.combat.CombatFeedKind;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

/** One successful combat transition. A missing actor represents environmental damage. */
public record KillFeedS2CPacket(String map, CombatFeedKind kind, UUID actor, String actorName, UUID target, String targetName, ItemStack weapon) {
    public KillFeedS2CPacket { weapon = weapon.copy(); }
    public static void encode(KillFeedS2CPacket p, FriendlyByteBuf b) {
        b.writeUtf(p.map, 128); b.writeEnum(p.kind);
        b.writeBoolean(p.actor != null);
        if (p.actor != null) b.writeUUID(p.actor);
        b.writeUtf(p.actorName, 64); b.writeUUID(p.target); b.writeUtf(p.targetName, 64);
        b.writeItem(p.weapon);
    }
    public static KillFeedS2CPacket decode(FriendlyByteBuf b) {
        return new KillFeedS2CPacket(b.readUtf(128), b.readEnum(CombatFeedKind.class), b.readBoolean() ? b.readUUID() : null,
                b.readUtf(64), b.readUUID(), b.readUtf(64), b.readItem());
    }
    public void handle(Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.enqueueWork(() -> BattlezoneClientPacketHandler.handle(this));
        context.setPacketHandled(true);
    }
}
