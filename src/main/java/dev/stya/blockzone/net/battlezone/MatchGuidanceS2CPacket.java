package dev.stya.blockzone.net.battlezone;

import dev.stya.blockzone.map.battlezone.BattlezoneMap.MatchPhase;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

/** Authoritative phase, zone countdown and individual danger state; presentation stays client-side. */
public record MatchGuidanceS2CPacket(String map, boolean active, MatchPhase phase, int stage,
                                     boolean shrinking, int remainingTicks, int totalTicks, boolean outside, java.util.List<Integer> sequenceTicks) {
    public MatchGuidanceS2CPacket { sequenceTicks = java.util.List.copyOf(sequenceTicks); }
    public static void encode(MatchGuidanceS2CPacket p, FriendlyByteBuf b) {
        b.writeUtf(p.map, 128); b.writeBoolean(p.active); b.writeEnum(p.phase); b.writeVarInt(p.stage);
        b.writeBoolean(p.shrinking); b.writeVarInt(p.remainingTicks); b.writeVarInt(p.totalTicks); b.writeBoolean(p.outside); b.writeCollection(p.sequenceTicks, (buffer, ticks) -> buffer.writeVarInt(ticks));
    }
    public static MatchGuidanceS2CPacket decode(FriendlyByteBuf b) {
        return new MatchGuidanceS2CPacket(b.readUtf(128), b.readBoolean(), b.readEnum(MatchPhase.class), b.readVarInt(), b.readBoolean(), b.readVarInt(), b.readVarInt(), b.readBoolean(), b.readList(FriendlyByteBuf::readVarInt));
    }
    public void handle(Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.enqueueWork(() -> BattlezoneClientPacketHandler.handle(this));
        context.setPacketHandled(true);
    }
}
