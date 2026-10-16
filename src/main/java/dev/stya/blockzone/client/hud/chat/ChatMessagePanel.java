package dev.stya.blockzone.client.hud.chat;

import dev.stya.blockzone.client.hud.common.HudCanvas;
import dev.stya.blockzone.client.hud.squad.SquadClientState;
import net.minecraft.client.gui.GuiGraphics;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class ChatMessagePanel {
    private final ChatMessageRow row = new ChatMessageRow();
    public void render(GuiGraphics graphics, List<ChatMessage> messages, UUID localPlayer, long now, float opacity) {
        try (var g = new HudCanvas(graphics)) {
            float width = Math.min(616, g.width - 152);
            var visible = new ArrayList<ChatMessageRow.Layout>();
            float height = 0;
            for (int i = messages.size() - 1; i >= 0; i--) {
                var message = messages.get(i);
                if (message.opacity(now) <= 0) continue;
                var layout = row.layout(g, message, width - 8);
                if (height + layout.height() > 192) break;
                visible.add(0, layout); height += layout.height();
            }
            if (visible.isEmpty() || opacity <= 0) return;
            g.rect(76, 94, width, height + 8, ChatMessageRow.alpha(0xCC10181E, opacity));
            float y = 98;
            for (var layout : visible) {
                var sender = layout.message().sender();
                boolean teammate = localPlayer.equals(sender) || SquadClientState.members().stream().anyMatch(m -> m.id().equals(sender));
                row.render(g, layout, 80, y, teammate ? 0xFF2DC653 : 0xFFF87882,
                        layout.message().opacity(now) * opacity);
                y += layout.height();
            }
        }
    }
}
