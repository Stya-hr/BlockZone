package dev.stya.blockzone.client.hud.chat;

import dev.stya.blockzone.client.hud.common.HudCanvas;
import dev.stya.blockzone.client.hud.squad.SquadClientState;
import net.minecraft.client.gui.GuiGraphics;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class ChatMessagePanel {
    public static final int X = 76, Y = 94, WIDTH = 616, EXPANDED_HEIGHT = 300, INPUT_HEIGHT = 40;
    private final ChatMessageRow row = new ChatMessageRow();
    public void render(GuiGraphics graphics, List<ChatMessage> messages, UUID localPlayer, long now, float opacity) {
        render(graphics, messages, localPlayer, now, opacity, false, 0);
    }
    public void render(GuiGraphics graphics, List<ChatMessage> messages, UUID localPlayer, long now, float opacity,
                       boolean expanded, int scroll) {
        try (var g = new HudCanvas(graphics)) {
            float width = Math.min(WIDTH, g.width - X * 2);
            var visible = new ArrayList<ChatMessageRow.Layout>();
            float height = 0;
            for (int i = messages.size() - 1 - scroll; i >= 0; i--) {
                var message = messages.get(i);
                if (!expanded && message.opacity(now) <= 0) continue;
                var layout = row.layout(g, message, width - 8);
                if (height + layout.height() > (expanded ? EXPANDED_HEIGHT - INPUT_HEIGHT - 12 : 192)) break;
                visible.add(0, layout); height += layout.height();
            }
            if ((!expanded && visible.isEmpty()) || opacity <= 0) return;
            g.rect(X, Y, width, expanded ? EXPANDED_HEIGHT : height + 8, ChatMessageRow.alpha(HudCanvas.PANEL_BACKGROUND, opacity));
            if (expanded) g.rect(X + 4, Y + EXPANDED_HEIGHT - INPUT_HEIGHT, width - 8, 1, 0x66F3F7F8);
            float y = Y + 4;
            for (var layout : visible) {
                var sender = layout.message().sender();
                boolean teammate = localPlayer.equals(sender) || SquadClientState.members().stream().anyMatch(m -> m.id().equals(sender));
                row.render(g, layout, 80, y, teammate ? 0xFF2DC653 : 0xFFF87882,
                        (expanded ? 1 : layout.message().opacity(now)) * opacity);
                y += layout.height();
            }
        }
    }
}
