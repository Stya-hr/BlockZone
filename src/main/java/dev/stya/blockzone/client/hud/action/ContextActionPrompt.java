package dev.stya.blockzone.client.hud.action;

import dev.stya.blockzone.client.hud.common.HudCanvas;

/** One compact contextual key/action line, shared by flight and nearby interactions. */
public final class ContextActionPrompt {
    public void render(HudCanvas g, ActionStatus status, float centerX, float y) {
        int keyWidth = g.keyWidth(status.key());
        int gap = keyWidth == 0 ? 0 : 10;
        var title = g.fit(status.title(), 420 - keyWidth - gap, 20);
        float x = centerX - (g.textWidth(title, 20) + keyWidth + gap) / 2;
        g.key(status.key(), Math.round(x), Math.round(y + 12));
        g.textWithShadow(title, x + keyWidth + gap, y + 12, 20, status.color());
    }
}
