package dev.stya.blockzone.client.hud.chat;

import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HudChatFilterTest {
    @Test void removesWrappedMatchStartButKeepsCommandsErrorsAndPlayerText() {
        var start = Component.translatable("commands.fpsm.debug.start.success", "flat160");
        assertTrue(HudChatFilter.replaced(start));
        assertTrue(HudChatFilter.replaced(Component.translatable("chat.type.admin", "Server", start)));
        assertFalse(HudChatFilter.replaced(Component.translatable("chat.type.admin", "Server",
                Component.translatable("commands.give.success.single"))));
        assertFalse(HudChatFilter.replaced(Component.translatable("commands.fpsm.team.join.failure.full")));
        assertFalse(HudChatFilter.replaced(Component.literal("commands.fpsm.debug.start.success")));
    }
}
