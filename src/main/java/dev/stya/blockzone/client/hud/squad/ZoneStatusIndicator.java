package dev.stya.blockzone.client.hud.squad;

import dev.stya.blockzone.client.hud.common.HudCanvas;
import dev.stya.blockzone.map.battlezone.BattlezoneMap.MatchPhase;
import dev.stya.blockzone.net.battlezone.MatchGuidanceS2CPacket;
import net.minecraft.network.chat.Component;

/** Zone progress bar, countdown and status above the optional minimap. */
public final class ZoneStatusIndicator {
    public void render(HudCanvas g, MatchGuidanceS2CPacket state, float seconds) {
        boolean minimap = dev.stya.blockzone.client.compat.XaeroMinimapCompat.active();
        boolean running = state != null && (state.phase() == MatchPhase.MATCH
                || minimap && !state.sequenceTicks().isEmpty());
        if (!minimap && !running) return;
        float x = SquadHudStyle.LEFT;
        float y = g.height - (SquadHudStyle.DESIGN_HEIGHT - SquadHudStyle.LOCAL_TOP) + LocalPlayerStatus.HEIGHT + 22;
        float width = 320;
        if (minimap) {
            y = dev.stya.blockzone.client.compat.XaeroMinimapCompat.top(g.height) - 55;
            width = dev.stya.blockzone.client.compat.XaeroMinimapCompat.width();
        }
        var timer = Component.literal(running ? formatTime(seconds) : "--:--");
        Component label = Component.translatable("hud.blockzone.zone_indicator." + (running && state.shrinking() ? "shrinking" : "waiting"));
        label = g.fit(label, 180, 16);
        g.textWithShadow(timer, x, y, 22, HudCanvas.TEXT);
        g.textWithShadow(label, x + width - g.textWidth(label, 16), y + 4, 16, HudCanvas.TEXT);

        float trackY = y + 28, trackHeight = 7;
        var ticks = running ? state.sequenceTicks() : java.util.List.<Integer>of();
        float filled = running ? width * sequenceProgress(state.stage(), state.shrinking(), seconds, ticks) : 0;
        g.rect(x, trackY, width, trackHeight, 0x9910181E);
        double total = ticks.stream().mapToDouble(t -> Math.max(0, t)).sum();
        float offset = 0;
        for (int i = 0; i < ticks.size() && total > 0; i++) {
            float segmentWidth = (float)(width * Math.max(0, ticks.get(i)) / total);
            int color = i % 2 == 0 ? 0xFFBFC8CE : 0xFF5797C5;
            g.rect(x + offset, trackY, segmentWidth, trackHeight, color);
            offset += segmentWidth;
            if (i % 2 == 1 && i + 1 < ticks.size()) {
                g.rect(x + offset - 1, trackY - 1, 2, trackHeight + 2, 0xFF10181E);
            }
        }
        // A bright moving edge keeps progress readable without a large panel background.
        float marker = x + Math.min(width - 2, filled);
        g.rect(marker - 1, trackY - 3, 4, trackHeight + 6, 0x9910181E);
        g.rect(marker, trackY - 2, 2, trackHeight + 4, HudCanvas.TEXT);
    }
    public static float sequenceProgress(int stage, boolean shrinking, float seconds, java.util.List<Integer> ticks) {
        double total = ticks.stream().mapToDouble(t -> Math.max(0, t)).sum();
        if (total <= 0) return 1;
        int current = Math.max(0, (stage - 1) * 2 + (shrinking ? 1 : 0));
        double elapsed = 0;
        for (int i = 0; i < ticks.size(); i++) {
            int duration = Math.max(0, ticks.get(i));
            if (i < current) elapsed += duration;
            else if (i == current) elapsed += duration * progress(seconds, duration);
        }
        return (float)Math.max(0, Math.min(1, elapsed / total));
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
