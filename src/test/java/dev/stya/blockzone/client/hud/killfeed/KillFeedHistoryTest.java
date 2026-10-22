package dev.stya.blockzone.client.hud.killfeed;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class KillFeedHistoryTest {
    @Test void keepsNewestFiveAndExpiresWithoutRendering() {
        // The queue treats payloads as opaque; lifetime tests need no Minecraft registries.
        var history = new KillFeedHistory();
        for (int i = 0; i < 7; i++) history.add(null, i * 100);
        var visible = history.visible(1000);
        assertEquals(5, visible.size());
        assertEquals(600, visible.get(0).lifetime().startedAt());
        assertEquals(200, visible.get(4).lifetime().startedAt());
        assertEquals(1, visible.get(0).lifetime().opacity(1000));
        assertTrue(visible.get(0).lifetime().opacity(6500) < 1);
        assertTrue(history.visible(6600).isEmpty());
        history.add(null, 10000);
        assertEquals(1, history.visible(10100).size());
        history.clear();
        assertTrue(history.visible(10100).isEmpty());
    }
}
