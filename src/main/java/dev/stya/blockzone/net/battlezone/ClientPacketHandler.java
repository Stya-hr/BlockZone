package dev.stya.blockzone.net.battlezone;

import dev.stya.blockzone.client.battlezone.ZoneClientState;
import dev.stya.blockzone.client.battlezone.DeploymentClientController;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

final class ClientPacketHandler {
    private ClientPacketHandler() {
    }

    static void handle(ZonePreviewS2CPacket packet) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ZoneClientState.applyZonePreview(packet));
    }

    static void handle(FlightStateS2CPacket packet) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> DeploymentClientController.apply(packet));
    }

    static void handle(ZoneStateS2CPacket packet) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ZoneClientState.apply(packet));
    }

    static void handle(BoundaryPreviewS2CPacket packet) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ZoneClientState.applyPreview(packet));
    }
}
