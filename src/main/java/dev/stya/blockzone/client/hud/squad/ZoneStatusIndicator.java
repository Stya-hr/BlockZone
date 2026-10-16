package dev.stya.blockzone.client.hud.squad;

import dev.stya.blockzone.client.hud.common.HudCanvas;
import dev.stya.blockzone.map.battlezone.BattlezoneMap.MatchPhase;
import dev.stya.blockzone.net.battlezone.MatchGuidanceS2CPacket;
import net.minecraft.network.chat.Component;

/** Zone progress bar, countdown and status directly beneath the local vitals. */
public final class ZoneStatusIndicator {
    public void render(HudCanvas g, MatchGuidanceS2CPacket state, float seconds) {
        if (state == null || state.phase() != MatchPhase.MATCH) return;
        float x = SquadHudStyle.LEFT;
        float y = g.height - (SquadHudStyle.DESIGN_HEIGHT - SquadHudStyle.LOCAL_TOP) + LocalPlayerStatus.HEIGHT + 22;
        int color = HudCanvas.TEXT;
        float width = 320;
        g.rect(x, y, width, 42, 0xB310181E);
        g.rect(x, y, 3, 42, color);
        var label = Component.translatable("hud.blockzone.zone_indicator." + (state.shrinking() ? "shrinking" : "waiting"));
        g.textWithShadow(g.fit(label, 200, 16), x + 12, y + 8, 16, color);
        g.right(Component.literal(formatTime(seconds)), x + width - 12, y + 4, 22, color);
        g.progress(x + 12, y + 32, width - 24, 4, progress(seconds, state.totalTicks()), color);
    }
    public static float progress(float seconds, int totalTicks) {
        if (totalTicks <= 0) return 1;
        if (!Float.isFinite(seconds)) return 0;
        return Math.max(0, Math.min(1, 1 - seconds * 20f / totalTicks));
    }

    public static String formatTime(float seconds) {
        int value = Float.isFinite(seconds) ? Math.max(0, (int)Math.ceil(seconds)) : 0;
        return String.format(java.util.Locale.ROOT, "%02d:%02d", value / 60, value % 60);
    }
}
