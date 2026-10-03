package dev.stya.blockzone.net.battlezone;

import dev.stya.blockzone.map.battlezone.BattlezoneMap;
import com.ptcrys.fpsmatch.core.FPSMCore;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public record ReleaseDeploymentC2SPacket() {
    public static void encode(ReleaseDeploymentC2SPacket packet, FriendlyByteBuf buffer) { }

    public static ReleaseDeploymentC2SPacket decode(FriendlyByteBuf buffer) {
        return new ReleaseDeploymentC2SPacket();
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            var player = context.getSender();
            if (player != null) {
                FPSMCore.getInstance().getMapByPlayerWithSpec(player)
                        .filter(BattlezoneMap.class::isInstance).map(BattlezoneMap.class::cast)
                        .ifPresent(map -> map.releaseDeployment(player));
            }
        });
        context.setPacketHandled(true);
    }
}
