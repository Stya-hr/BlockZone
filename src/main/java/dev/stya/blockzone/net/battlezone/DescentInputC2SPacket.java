package dev.stya.blockzone.net.battlezone;

import com.ptcrys.fpsmatch.core.FPSMCore;
import dev.stya.blockzone.map.battlezone.BattlezoneMap;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

/** Direction only; the server owns descent speed, orientation and flight state. */
public record DescentInputC2SPacket(int forward, int left) {
    public static void encode(DescentInputC2SPacket packet, FriendlyByteBuf buffer) {
        buffer.writeByte(packet.forward());
        buffer.writeByte(packet.left());
    }
    public static DescentInputC2SPacket decode(FriendlyByteBuf buffer) {
        return new DescentInputC2SPacket(buffer.readByte(), buffer.readByte());
    }
    public void handle(Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.enqueueWork(() -> {
            var player = context.getSender();
            if (player != null && forward >= -1 && forward <= 1 && left >= -1 && left <= 1) {
                FPSMCore.getInstance().getMapByPlayerWithSpec(player)
                        .filter(BattlezoneMap.class::isInstance).map(BattlezoneMap.class::cast)
                        .ifPresent(map -> map.controlDescent(player, forward, left));
            }
        });
        context.setPacketHandled(true);
    }
}
