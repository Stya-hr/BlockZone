package dev.stya.blockzone.client.hud.notice;

import dev.stya.blockzone.client.battlezone.DeploymentClientController;
import dev.stya.blockzone.client.battlezone.RescueClientState;
import dev.stya.blockzone.client.hud.common.HudCanvas;
import dev.stya.blockzone.map.battlezone.BattlezoneMap.MatchPhase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;

public final class ActionGuidancePanel {
    public record Guidance(Component title, Component detail, int color, KeyMapping key, Component keyLabel) {}
    public Guidance read(float partial) {
        var mc = Minecraft.getInstance();
        var state = NoticeClientState.guidance();
        if (state == null) return null;
        if (state.phase() == MatchPhase.SETTLEMENT) return text("settlement", "next_match", HudCanvas.TEXT);
        if (mc.player.isSpectator()) return text("eliminated", "spectating", HudCanvas.MUTED);
        var rescue = RescueClientState.snapshot(mc.player.getUUID());
        if (rescue != null && rescue.state() == 1)
            return text("wait_rescue", rescue.rescueTicks() > 0 ? "receiving_rescue" : "stay_alive", HudCanvas.RED);
        if (rescue != null && rescue.state() == 2) return text("finish_rescue", "hold_position", HudCanvas.GREEN);
        int flight = DeploymentClientController.state();
        if (flight != 0) return new Guidance(tr(flight == 1 ? "choose_drop" : "choose_landing"),
                tr(flight == 1 ? "transport" : flight == 2 ? "gliding" : "freefall"), HudCanvas.TEXT,
                flight == 1 ? DeploymentClientController.releaseKey() : DeploymentClientController.parachuteKey(),
                Component.translatable("hud.blockzone.flight." + (flight == 1 ? "jump" : flight == 2 ? "close" : "open")));
        if (state.outside()) return text("enter_zone", "outside_zone", HudCanvas.RED);
        float seconds = NoticeClientState.remainingSeconds(partial);
        return new Guidance(tr(state.shrinking() ? "zone_shrinking" : "loot_survive"),
                seconds > 0 ? tr(state.shrinking() ? "shrink_remaining" : "shrink_in", String.format(java.util.Locale.ROOT, "%.1f", seconds))
                        : tr("watch_zone"), state.shrinking() ? 0xFFFFC76D : HudCanvas.TEXT, null, Component.empty());
    }
    private Guidance text(String title, String detail, int color) { return new Guidance(tr(title), tr(detail), color, null, Component.empty()); }
    public void render(HudCanvas g, Guidance guidance) {
        if (guidance == null) return;
        float y = 100;
        var title = g.fit(guidance.title(), 620, 22);
        var detail = g.fit(guidance.detail(), 620, 16);
        g.textWithShadow(title, (g.width - g.textWidth(title, 22)) / 2, y, 22, guidance.color());
        if (guidance.key() == null) {
            g.textWithShadow(detail, (g.width - g.textWidth(detail, 16)) / 2, y + 34, 16, HudCanvas.MUTED);
        } else {
            var label = g.fit(Component.empty().append(guidance.detail()).append("  ").append(guidance.keyLabel()), 580, 16);
            int keyWidth = g.keyWidth(guidance.key());
            float x = (g.width - keyWidth - 10 - g.textWidth(label, 16)) / 2;
            g.key(guidance.key(), Math.round(x), Math.round(y + 32));
            g.textWithShadow(label, x + keyWidth + 10, y + 34, 16, HudCanvas.MUTED);
        }
    }
    private static Component tr(String key, Object... args) { return Component.translatable("hud.blockzone.guide." + key, args); }
}
