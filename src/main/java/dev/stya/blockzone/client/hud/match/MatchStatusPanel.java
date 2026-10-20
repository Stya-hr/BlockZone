package dev.stya.blockzone.client.hud.match;

import com.ptcrys.fpsmatch.common.client.FPSMClient;
import dev.stya.blockzone.client.hud.common.HudCanvas;
import dev.stya.blockzone.net.battlezone.MatchGuidanceS2CPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** Right-aligned icon strip matching the QHD HUD reference. */
public final class MatchStatusPanel {
    private static final ResourceLocation TEAMS = icon("teams"), PLAYERS = icon("players"), KILLS = icon("kills");
    public void render(HudCanvas g, MatchGuidanceS2CPacket state) {
        if (state == null || !state.active()) return;
        var client = FPSMClient.getGlobalData();
        var value = MatchStatusSnapshot.capture(client.getTeams(), state.map(), Minecraft.getInstance().player.getUUID());
        String teams = Integer.toString(value.teams()), players = Integer.toString(value.players());
        String kills = value.participant() ? Integer.toString(value.kills()) : "—";
        float numberWidth = Math.max(g.textWidth(Component.literal(teams), 24),
                Math.max(g.textWidth(Component.literal(players), 24), g.textWidth(Component.literal(kills), 24)));
        float cellWidth = Math.max(82, 54 + numberWidth);
        float width = cellWidth * 3, height = 44, x = g.width - 65 - width, y = 48;
        g.rect(x, y, width, height, HudCanvas.PANEL_BACKGROUND);
        g.rect(x, y, width, 1, 0x66F3F7F8);
        g.rect(x, y + height - 1, width, 1, 0x55F3F7F8);
        g.rect(x, y + 1, 1, height - 2, 0x55F3F7F8);
        g.rect(x + width - 1, y + 1, 1, height - 2, 0x55F3F7F8);
        counter(g, x, y, cellWidth, TEAMS, teams);
        counter(g, x + cellWidth, y, cellWidth, PLAYERS, players);
        counter(g, x + cellWidth * 2, y, cellWidth, KILLS, kills);
        g.rect(x + cellWidth, y + 12, 1, height - 24, 0x33F3F7F8);
        g.rect(x + cellWidth * 2, y + 12, 1, height - 24, 0x33F3F7F8);
    }

    private static ResourceLocation icon(String name) {
        return new ResourceLocation("blockzone", "textures/hud/match/" + name + ".png");
    }

    private static void counter(HudCanvas g, float x, float y, float width, ResourceLocation icon, String value) {
        g.gui.blit(icon, (int)x + 10, (int)y + 10, 24, 24, 0, 0, 64, 64, 64, 64);
        g.right(Component.literal(value), x + width - 12, y + 10, 24, HudCanvas.TEXT);
    }
}
