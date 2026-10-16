package dev.stya.blockzone.client.hud.notice;

import dev.stya.blockzone.client.hud.common.HudCanvas;
import dev.stya.blockzone.net.battlezone.HudAnnouncementS2CPacket;

public final class AnnouncementBanner {
    public void render(HudCanvas g, HudAnnouncementS2CPacket notice, float opacity) {
        if (notice == null || opacity <= 0) return;
        float width = Math.min(680, g.width - 152), x = (g.width - width) / 2, y = g.height * .19f;
        g.rect(x, y, width, 104, alpha(HudCanvas.PANEL_BACKGROUND, opacity));
        g.rect(x + 32, y, width - 64, 1, alpha(notice.color(), opacity));
        g.rect(x + 32, y + 103, width - 64, 1, alpha(notice.color(), opacity));
        var title = g.fit(notice.title(), width - 48, 32);
        var detail = g.fit(notice.detail(), width - 48, 18);
        g.text(title, (g.width - g.textWidth(title, 32)) / 2, y + 20, 32, alpha(notice.color(), opacity));
        g.text(detail, (g.width - g.textWidth(detail, 18)) / 2, y + 67, 18, alpha(HudCanvas.TEXT, opacity));
    }
    static int alpha(int color, float opacity) { return (color & 0xFFFFFF) | (Math.max(1, Math.round((color >>> 24) * opacity)) << 24); }
}
