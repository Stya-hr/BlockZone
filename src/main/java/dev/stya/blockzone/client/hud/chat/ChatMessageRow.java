package dev.stya.blockzone.client.hud.chat;

import dev.stya.blockzone.client.hud.common.HudCanvas;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import java.util.ArrayList;
import java.util.List;

public final class ChatMessageRow {
    private final ChatNameTag tag = new ChatNameTag();
    public record Layout(ChatMessage message, float tagWidth, List<String> lines) {
        public float height() { return lines.size() * 24 + 8; }
    }
    public Layout layout(HudCanvas g, ChatMessage message, float width) {
        float tagWidth = message.name().isEmpty() ? 0 : tag.width(g, message.name()) + 8;
        var lines = new ArrayList<String>();
        // Use the same font advances as ModernUiGraphics, including CJK wrapping.
        for (var line : Minecraft.getInstance().font.split(Component.literal(message.body()),
                Math.max(1, (int)((width - tagWidth) * 8 / 20)))) {
            var text = new StringBuilder();
            line.accept((index, style, codepoint) -> { text.appendCodePoint(codepoint); return true; });
            lines.add(text.toString());
            if (lines.size() == 6) break;
        }
        if (lines.isEmpty()) lines.add("");
        return new Layout(message, tagWidth, List.copyOf(lines));
    }
    public void render(HudCanvas g, Layout layout, float x, float y, int nameColor, float opacity) {
        if (layout.tagWidth() > 0) tag.render(g, layout.message().name(), x, y,
                layout.tagWidth() - 8, nameColor, opacity);
        for (int i = 0; i < layout.lines().size(); i++)
            g.text(layout.lines().get(i), x + layout.tagWidth(), y + 4 + i * 24, 20,
                    alpha(HudCanvas.TEXT, opacity));
    }
    static int alpha(int color, float opacity) {
        return (color & 0xFFFFFF) | (Math.max(1, Math.round((color >>> 24) * opacity)) << 24);
    }
}
