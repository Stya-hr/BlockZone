package dev.stya.blockzone.client.hud.notice;

import com.ptcrys.fpsmatch.common.client.FPSMClient;
import dev.stya.blockzone.BlockZone;
import dev.stya.blockzone.map.battlezone.BattlezoneMap;
import dev.stya.blockzone.net.battlezone.*;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BlockZone.MOD_ID, value = Dist.CLIENT)
public final class NoticeClientState {
    private static MatchGuidanceS2CPacket guidance;
    private static long receivedTick;
    private static HudAnnouncementS2CPacket announcement;
    private static AnnouncementLifetime lifetime;
    private static long generation;
    private NoticeClientState() {}
    private static boolean matches(String map) {
        var data = FPSMClient.getGlobalData();
        return data.isCurrentGameType(BattlezoneMap.GAME_TYPE) && data.isCurrentMap(map);
    }
    public static void apply(MatchGuidanceS2CPacket packet) {
        if (!matches(packet.map())) return;
        if (!packet.active()) { clear(); return; }
        guidance = packet;
        var level = Minecraft.getInstance().level;
        receivedTick = level == null ? 0 : level.getGameTime();
    }
    public static void announce(HudAnnouncementS2CPacket packet) {
        if (!matches(packet.map())) return;
        long now = net.minecraft.Util.getMillis();
        if (announcement != null && matches(announcement.map()) && now < lifetime.startedAt() + lifetime.durationMillis()
                && (announcement.key().equals(packet.key()) || announcement.priority() > packet.priority())) return;
        announcement = packet;
        lifetime = new AnnouncementLifetime(now, Math.max(20, Math.min(200, packet.durationTicks())) * 50L);
    }
    static MatchGuidanceS2CPacket guidance() { return guidance != null && matches(guidance.map()) ? guidance : null; }
    static String scope() { return (guidance == null ? "" : guidance.map()) + ":" + generation; }
    static float remainingSeconds(float partial) {
        var level = Minecraft.getInstance().level;
        return guidance == null || level == null ? 0 : Math.max(0, (guidance.remainingTicks() - (level.getGameTime() - receivedTick + partial)) / 20f);
    }
    static HudAnnouncementS2CPacket announcement() {
        return announcement != null && matches(announcement.map()) ? announcement : null;
    }
    static float opacity() { return lifetime == null ? 0 : lifetime.opacity(net.minecraft.Util.getMillis()); }
    private static void clear() { guidance = null; announcement = null; lifetime = null; receivedTick = 0; generation++; }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { clear(); }
    @SubscribeEvent public static void reset(com.ptcrys.fpsmatch.common.client.event.FPSMClientResetEvent event) { clear(); }
}
