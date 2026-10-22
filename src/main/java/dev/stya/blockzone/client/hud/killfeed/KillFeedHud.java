package dev.stya.blockzone.client.hud.killfeed;

import com.ptcrys.fpsmatch.common.client.FPSMClient;
import dev.stya.blockzone.BlockZone;
import dev.stya.blockzone.client.hud.common.HudCanvas;
import dev.stya.blockzone.client.hud.squad.SquadClientState;
import dev.stya.blockzone.client.inventory.GunHudIcon;
import dev.stya.blockzone.map.battlezone.BattlezoneMap;
import dev.stya.blockzone.net.battlezone.KillFeedS2CPacket;
import dev.stya.blockzone.combat.CombatFeedKind;
import java.util.UUID;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BlockZone.MOD_ID, value = Dist.CLIENT)
public final class KillFeedHud {
    private static final KillFeedHistory HISTORY = new KillFeedHistory();
    private static final ResourceLocation ELIMINATION = new ResourceLocation("blockzone", "textures/hud/match/kills.png");
    private static String scope = "";
    private KillFeedHud() { }
    private static boolean matches(String map) {
        var data = FPSMClient.getGlobalData();
        return data.isCurrentGameType(BattlezoneMap.GAME_TYPE) && data.isCurrentMap(map);
    }
    public static void receive(KillFeedS2CPacket packet) {
        if (Minecraft.getInstance().player == null || !matches(packet.map())) return;
        if (!scope.equals(packet.map())) clear();
        scope = packet.map();
        HISTORY.add(packet, net.minecraft.Util.getMillis());
    }
    public static void render(HudCanvas g, dev.stya.blockzone.net.battlezone.MatchGuidanceS2CPacket state) {
        if (state == null || !state.active() || !matches(scope)) { clear(); return; }
        long now = net.minecraft.Util.getMillis();
        float y = 112, right = g.width - 65;
        for (var entry : HISTORY.visible(now)) {
            var packet = entry.notice();
            float opacity = entry.lifetime().opacity(now);
            if (opacity < 1 / 255f) continue;
            Component killer = packet.actor() == null ? Component.translatable(packet.kind() == CombatFeedKind.REVIVED
                    ? "hud.blockzone.kill_feed.system" : "hud.blockzone.kill_feed.environment") : Component.literal(packet.actorName());
            killer = g.fit(killer, 210, 22);
            var victim = g.fit(Component.literal(packet.targetName()), 210, 22);
            var status = Component.translatable("hud.blockzone.kill_feed." + packet.kind().name().toLowerCase(Locale.ROOT));
            float statusWidth = g.textWidth(status, 16);
            float killerWidth = g.textWidth(killer, 22), victimWidth = g.textWidth(victim, 22);
            float width = killerWidth + victimWidth + statusWidth + 128, x = right - width;
            g.rect(x, y, width, 34, alpha(HudCanvas.PANEL_BACKGROUND, opacity));
            g.text(killer, x + 8, y + 6, 22, alpha(color(packet.actor()), opacity));
            float iconX = x + 20 + killerWidth;
            g.gui.setColor(1, 1, 1, opacity);
            g.gui.pose().pushPose();
            g.gui.pose().translate(iconX + 76, y + 3, 0);
            g.gui.pose().scale(-1, 1, 1);
            boolean gun = packet.kind() != CombatFeedKind.REVIVED && GunHudIcon.draw(g.gui, packet.weapon(), 0, 0, 76, 28, opacity);
            g.gui.pose().popPose();
            if (packet.kind() == CombatFeedKind.REVIVED) {
                g.text("+", iconX + 27, y + 1, 30, alpha(HudCanvas.GREEN, opacity));
            } else if (!gun) {
                if (packet.weapon().isEmpty()) g.gui.blit(ELIMINATION, (int)iconX + 26, (int)y + 5, 24, 24, 0, 0, 64, 64, 64, 64);
                else {
                    g.gui.pose().pushPose();
                    g.gui.pose().translate(iconX + 26, y + 5, 0); g.gui.pose().scale(1.5f, 1.5f, 1);
                    g.gui.renderItem(packet.weapon(), 0, 0); g.gui.pose().popPose();
                }
            }
            g.gui.setColor(1, 1, 1, 1);
            int statusColor = switch (packet.kind()) {
                case DOWNED -> 0xFFFFDD55;
                case ELIMINATED -> HudCanvas.RED;
                case REVIVED -> HudCanvas.GREEN;
            };
            g.text(status, iconX + 88, y + 9, 16, alpha(statusColor, opacity));
            g.right(victim, right - 8, y + 6, 22, alpha(color(packet.target()), opacity));
            y += 39;
        }
    }
    private static int color(UUID id) {
        if (id == null) return HudCanvas.MUTED;
        if (id.equals(Minecraft.getInstance().player.getUUID())) return 0xFFFFDD55;
        return SquadClientState.members().stream().anyMatch(m -> m.id().equals(id)) ? HudCanvas.GREEN : HudCanvas.RED;
    }
    private static int alpha(int color, float opacity) {
        return (color & 0xFFFFFF) | Math.round((color >>> 24) * opacity) << 24;
    }
    public static void clear() { HISTORY.clear(); scope = ""; }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { clear(); }
    @SubscribeEvent public static void reset(com.ptcrys.fpsmatch.common.client.event.FPSMClientResetEvent event) { clear(); }
}
