package dev.stya.blockzone.client.hud.chat;

import dev.stya.blockzone.client.hud.common.HudCanvas;
import net.minecraft.network.chat.Component;

public final class ChatNameTag {
    public float width(HudCanvas g, String name) {
        return Math.min(180, g.textWidth(Component.literal(name), 20) + 6);
    }
    public void render(HudCanvas g, String name, float x, float y, float width, int color, float opacity) {
        g.rect(x, y, width, 28, ChatMessageRow.alpha(color, opacity));
        g.text(g.fit(Component.literal(name), width - 6, 20), x + 3, y + 4, 20,
                ChatMessageRow.alpha(HudCanvas.TEXT, opacity));
    }
}
