package dev.stya.blockzone.chat;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MatchChatTest {
    @Test void rejectsInvalidTextAndCommandsAtPacketBoundary() {
        assertEquals("中文 message", MatchChat.normalize("  中文 message  "));
        assertEquals("", MatchChat.normalize("   "));
        assertEquals("", MatchChat.normalize("/say bypass"));
        assertEquals("", MatchChat.normalize("line\nbreak"));
        assertEquals("", MatchChat.normalize("\u00a7aformat"));
        assertEquals("", MatchChat.normalize("x".repeat(257)));
        assertEquals(256, MatchChat.normalize("x".repeat(256)).length());
    }
}
