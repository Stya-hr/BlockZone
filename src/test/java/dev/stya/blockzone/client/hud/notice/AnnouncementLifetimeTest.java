package dev.stya.blockzone.client.hud.notice;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AnnouncementLifetimeTest {
    @Test void fadesBothEndsAndExpiresWithoutSlidingOrRepeating() {
        var life = new AnnouncementLifetime(1000, 3000);
        assertEquals(0, life.opacity(999));
        assertEquals(0, life.opacity(1000));
        assertEquals(.5f, life.opacity(1060));
        assertEquals(1, life.opacity(1120));
        assertEquals(1, life.opacity(3000));
        assertEquals(.5f, life.opacity(3890));
        assertEquals(0, life.opacity(4000));
        assertEquals(0, life.opacity(5000));
    }
}
