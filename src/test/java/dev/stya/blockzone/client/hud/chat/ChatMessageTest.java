package dev.stya.blockzone.client.hud.chat;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ChatMessageTest {
    @Test void messagesHoldThenFadeAndExpire() {
        var message = new ChatMessage(null, "Player01", "Hello", 1000);
        assertEquals(1, message.opacity(1000));
        assertEquals(1, message.opacity(9000));
        assertEquals(.5f, message.opacity(10000));
        assertEquals(0, message.opacity(11000));
        assertEquals(0, message.opacity(12000));
    }
}
