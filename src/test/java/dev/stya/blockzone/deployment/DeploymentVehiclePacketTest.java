package dev.stya.blockzone.deployment;

import dev.stya.blockzone.net.battlezone.DeploymentVehicleS2CPacket;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DeploymentVehiclePacketTest {
    @Test
    void activeTransportRetainsRouteAndClockWithoutAnyPassengerIds() {
        var packet = new DeploymentVehicleS2CPacket(ResourceLocation.fromNamespaceAndPath("minecraft", "overworld"),
                new FlightRoute(12.25, 300, -40, -60, 300, 10.5, .5), 6000);
        assertEquals(packet, roundTrip(packet));
    }

    @Test
    void deploymentEndClearsTransportWithoutASyntheticRoute() {
        var packet = new DeploymentVehicleS2CPacket(ResourceLocation.fromNamespaceAndPath("minecraft", "the_nether"),
                null, 0);
        assertEquals(packet, roundTrip(packet));
        assertNull(roundTrip(packet).route());
    }

    private DeploymentVehicleS2CPacket roundTrip(DeploymentVehicleS2CPacket packet) {
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            DeploymentVehicleS2CPacket.encode(packet, buffer);
            var decoded = DeploymentVehicleS2CPacket.decode(buffer);
            assertEquals(0, buffer.readableBytes());
            return decoded;
        } finally {
            buffer.release();
        }
    }
}
