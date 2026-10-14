package dev.stya.blockzone.net.battlezone;

import dev.stya.blockzone.squad.SquadMemberStatus;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public record SquadStateS2CPacket(String map, String team, List<SquadMemberStatus> members) {
    public static final SquadStateS2CPacket EMPTY = new SquadStateS2CPacket("", "", List.of());
    public SquadStateS2CPacket { members = List.copyOf(members); }
    public static void encode(SquadStateS2CPacket packet, FriendlyByteBuf b) {
        b.writeUtf(packet.map); b.writeUtf(packet.team);
        b.writeCollection(packet.members, (out, m) -> {
            out.writeUUID(m.id()); out.writeUtf(m.name()); out.writeVarInt(m.number());
            out.writeBoolean(m.online()); out.writeBoolean(m.living());
            out.writeFloat(m.health()); out.writeFloat(m.maxHealth()); out.writeFloat(m.armor());
            out.writeFloat(m.platePoints()); out.writeVarInt(m.plateSlots());
            out.writeVarInt(m.rescueState()); out.writeVarInt(m.bleedTicks()); out.writeVarInt(m.bleedTotalTicks());
            out.writeVarInt(m.rescueTicks()); out.writeVarInt(m.rescueTotalTicks()); out.writeBoolean(m.plating());
        });
    }
    public static SquadStateS2CPacket decode(FriendlyByteBuf b) {
        return new SquadStateS2CPacket(b.readUtf(), b.readUtf(), b.readList(in -> new SquadMemberStatus(
                in.readUUID(), in.readUtf(), in.readVarInt(), in.readBoolean(), in.readBoolean(),
                in.readFloat(), in.readFloat(), in.readFloat(), in.readFloat(), in.readVarInt(),
                in.readVarInt(), in.readVarInt(), in.readVarInt(), in.readVarInt(), in.readVarInt(), in.readBoolean())));
    }
    public void handle(Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.enqueueWork(() -> BattlezoneClientPacketHandler.handle(this));
        context.setPacketHandled(true);
    }
}
