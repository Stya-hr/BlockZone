package dev.stya.blockzone.client.hud.action;

import dev.stya.blockzone.client.hud.common.HudCanvas;
import java.util.Locale;
import net.minecraft.network.chat.Component;

/** Reusable operation/key-prompt card; it never reads or changes gameplay state. */
public final class ActionProgressPanel {
    public void render(HudCanvas g, ActionStatus status, float centerX, float y) {
        int x = Math.round(centerX - 210);
        boolean timed = status.progress() != null;
        g.panel(x, y, 420, timed ? 80 : 64);
        g.rect(x, y + 10, 3, (timed ? 80 : 64) - 20, status.color());
        int keyWidth = g.key(status.key(), x + 16, Math.round(y + 12));
        float labelX = x + 16 + (keyWidth == 0 ? 0 : keyWidth + 10);
        g.text(g.fit(status.title(), x + (timed ? 316 : 404) - labelX, 20), labelX, y + 12, 20, HudCanvas.TEXT);
        g.text(g.fit(status.detail(), 388, 14), x + 16, y + 37, 14, HudCanvas.MUTED);
        if (timed) {
            g.right(Component.literal(String.format(Locale.ROOT, "%.1f s", status.progress().seconds())), x + 404, y + 12, 22, status.color());
            g.progress(x + 16, y + 60, 388, 7, status.progress().fraction(), status.color());
        }
    }
}
