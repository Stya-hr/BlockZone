package dev.stya.blockzone.client.hud.squad;

import dev.stya.blockzone.client.hud.common.HudCanvas;
import dev.stya.blockzone.map.battlezone.BattlezoneMap.MatchPhase;
import dev.stya.blockzone.net.battlezone.MatchGuidanceS2CPacket;
import net.minecraft.network.chat.Component;

/** Compact phase marker, countdown and zone status directly beneath the local vitals. */
public final class ZoneStatusIndicator {
    public void render(HudCanvas g, MatchGuidanceS2CPacket state, float seconds) {
        if (state == null || state.phase() != MatchPhase.MATCH) return;
        float x = SquadHudStyle.LEFT;
        float y = g.height - (SquadHudStyle.DESIGN_HEIGHT - SquadHudStyle.LOCAL_TOP) + LocalPlayerStatus.HEIGHT + 22;
        int color = state.outside() ? HudCanvas.RED : state.shrinking() ? 0xFFFFC76D : HudCanvas.TEXT;
        var graphics = (dev.stya.blockzone.client.ui.ModernUiGraphics)g.gui;
        float cx = x + 14, cy = y + 14, radius = 13;
        for (int i = 0; i < 32; i++) {
            double a = i * Math.PI / 16, b = (i + 1) * Math.PI / 16;
            graphics.line(cx + (float)Math.cos(a) * radius, cy + (float)Math.sin(a) * radius,
                    cx + (float)Math.cos(b) * radius, cy + (float)Math.sin(b) * radius, 1.5f, color);
        }
        var number = Component.literal(Integer.toString(state.stage()));
        g.text(number, cx - g.textWidth(number, 15) / 2, y + 6, 15, color);
        var timer = Component.literal(formatTime(seconds));
        g.textWithShadow(timer, x + 40, y + 2, 24, color);
        var label = Component.translatable("hud.blockzone.zone_indicator." + (state.shrinking() ? "shrinking" : "waiting"));
        g.textWithShadow(label, x + 40 + g.textWidth(timer, 24) + 14, y + 7, 16, color);
    }
    public static String formatTime(float seconds) {
        int value = Float.isFinite(seconds) ? Math.max(0, (int)Math.ceil(seconds)) : 0;
        return String.format(java.util.Locale.ROOT, "%02d:%02d", value / 60, value % 60);
    }
}
