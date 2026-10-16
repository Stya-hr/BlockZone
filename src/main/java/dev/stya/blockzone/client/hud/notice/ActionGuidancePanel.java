package dev.stya.blockzone.client.hud.notice;

import dev.stya.blockzone.client.battlezone.DeploymentClientController;
import dev.stya.blockzone.client.battlezone.RescueClientState;
import dev.stya.blockzone.client.hud.common.HudCanvas;
import dev.stya.blockzone.map.battlezone.BattlezoneMap.MatchPhase;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public final class ActionGuidancePanel {
    private final BriefGuidance brief = new BriefGuidance();
    public record Guidance(Component title, Component detail, int color) {}
    public Guidance read(float partial) {
        var mc = Minecraft.getInstance();
        var state = NoticeClientState.guidance();
        if (state == null) return null;
        int flight = DeploymentClientController.state();
        var rescue = RescueClientState.snapshot(mc.player.getUUID());
        boolean showLoot = brief.visible(NoticeClientState.scope(), !mc.player.isSpectator() && flight == 0
                && rescue == null && !state.outside()
                && (state.phase() == MatchPhase.MATCH || state.phase() == MatchPhase.DEPLOYMENT), net.minecraft.Util.getMillis());
        if (state.phase() == MatchPhase.SETTLEMENT || mc.player.isSpectator()) return null;
        if (rescue != null && rescue.state() == 1)
            return text("wait_rescue", rescue.rescueTicks() > 0 ? "receiving_rescue" : "stay_alive", HudCanvas.RED);
        if (rescue != null && rescue.state() == 2) return text("finish_rescue", "hold_position", HudCanvas.GREEN);
        if (flight != 0) return null;
        if (state.outside()) return text("enter_zone", "outside_zone", HudCanvas.RED);
        return showLoot ? text("loot_survive", "watch_zone", HudCanvas.TEXT) : null;
    }
    private Guidance text(String title, String detail, int color) { return new Guidance(tr(title), tr(detail), color); }
    public void render(HudCanvas g, Guidance guidance) {
        if (guidance == null) return;
        float y = 100;
        var title = g.fit(guidance.title(), 620, 22);
        var detail = g.fit(guidance.detail(), 620, 16);
        g.textWithShadow(title, (g.width - g.textWidth(title, 22)) / 2, y, 22, guidance.color());
        g.textWithShadow(detail, (g.width - g.textWidth(detail, 16)) / 2, y + 34, 16, HudCanvas.MUTED);
    }
    private static Component tr(String key, Object... args) { return Component.translatable("hud.blockzone.guide." + key, args); }
}
