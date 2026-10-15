package dev.stya.blockzone.client.hud.action;

import dev.stya.blockzone.client.hud.common.HudCanvas;
import java.util.Locale;
import net.minecraft.network.chat.Component;

/** Reusable operation progress and unframed key prompt; reads no gameplay state. */
public final class ActionProgressPanel {
    public void render(HudCanvas g, ActionStatus status, float centerX, float y) {
        var title = g.fit(status.title(), 388, 20);
        float titleX = centerX - g.textWidth(title, 20) / 2;
        if (status.progress() == null) {
            int keyWidth = g.keyWidth(status.key());
            int gap = keyWidth == 0 ? 0 : 10;
            title = g.fit(status.title(), 420 - keyWidth - gap, 20);
            float x = centerX - (g.textWidth(title, 20) + keyWidth + gap) / 2;
            dev.stya.blockzone.client.inventory.InventoryPromptRenderer.softShadow(g.gui, Math.round(x), Math.round(y + 12),
                    Math.round(g.textWidth(title, 20) + keyWidth + gap), 20);
            g.key(status.key(), Math.round(x), Math.round(y + 12));
            float labelX = x + keyWidth + gap;
            g.text(title, labelX, y + 12, 20, HudCanvas.TEXT);
            return;
        }
        float x = centerX - 210;
        var seconds = Component.literal(String.format(Locale.ROOT, "  %.1f s", status.progress().seconds()));
        float secondsWidth = g.textWidth(seconds, 20);
        title = g.fit(status.title(), 388 - secondsWidth, 20);
        var label = Component.empty().append(title).append(seconds);
        g.panel(x, y, 420, 64);
        g.text(label, centerX - g.textWidth(label, 20) / 2, y + 12, 20, HudCanvas.TEXT);
        g.progress(x + 16, y + 44, 388, 7, status.progress().fraction(), status.color());
    }
}
