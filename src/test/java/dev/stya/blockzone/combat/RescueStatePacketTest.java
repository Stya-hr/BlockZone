package dev.stya.blockzone.combat;

import dev.stya.blockzone.net.battlezone.RescueStateS2CPacket;
import io.netty.buffer.Unpooled;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RescueStatePacketTest {
    @Test void customDurationsAndDistanceSurviveNetworkRoundTrip() {
        for (int state = 0; state <= 2; state++) {
            var packet = new RescueStateS2CPacket(UUID.randomUUID(), state, 450, 37, 900, 75, 4.5);
            var buffer = new FriendlyByteBuf(Unpooled.buffer());
            try {
                RescueStateS2CPacket.encode(packet, buffer);
                assertEquals(packet, RescueStateS2CPacket.decode(buffer));
                assertEquals(0, buffer.readableBytes());
            } finally { buffer.release(); }
        }
    }
}
