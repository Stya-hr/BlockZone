package dev.stya.blockzone.client.hud.action;

import dev.stya.blockzone.client.hud.common.HudCanvas;

/** Reusable operation progress and unframed key prompt; reads no gameplay state. */
public final class ActionProgressPanel {
    public void render(HudCanvas g, ActionStatus status, float centerX, float y) {
        var title = g.fit(status.title(), 388, 20);
        float titleX = centerX - g.textWidth(title, 20) / 2;
        if (status.progress() == null) {
            int keyWidth = g.key(status.key(), Math.round(titleX - 40), Math.round(y + 12));
            g.text(title, titleX - 40 + (keyWidth == 0 ? 40 : keyWidth + 10), y + 12, 20, HudCanvas.TEXT);
            return;
        }
        float x = centerX - 210;
        g.panel(x, y, 420, 64);
        g.text(title, titleX, y + 12, 20, HudCanvas.TEXT);
        g.progress(x + 16, y + 44, 388, 7, status.progress().fraction(), status.color());
    }
}
