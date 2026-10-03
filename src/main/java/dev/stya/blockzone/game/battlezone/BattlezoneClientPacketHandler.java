package dev.stya.blockzone.game.battlezone;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

final class BattlezoneClientPacketHandler {
    private BattlezoneClientPacketHandler() {
    }

    static void handle(BattlezoneFlightStateS2CPacket packet) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> BattlezoneDeploymentClient.apply(packet));
    }

    static void handle(BattlezoneZoneStateS2CPacket packet) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> BattlezoneClientState.apply(packet));
    }

    static void handle(BattlezoneBoundaryPreviewS2CPacket packet) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> BattlezoneClientState.applyPreview(packet));
    }
}
