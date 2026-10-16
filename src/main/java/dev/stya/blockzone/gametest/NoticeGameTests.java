package dev.stya.blockzone.gametest;

import dev.stya.blockzone.map.battlezone.BattlezoneMap;
import dev.stya.blockzone.net.battlezone.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.gametest.*;

@GameTestHolder("blockzone")
@PrefixGameTestTemplate(false)
public final class NoticeGameTests {
    @GameTest(template = "empty")
    public static void announcementAndGuidancePacketsPreserveScopeAndContent(GameTestHelper helper) {
        var buffer = new FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        try {
            var announcement = new HudAnnouncementS2CPacket("arena", "zone_shrinking", Component.literal("安全区开始收缩"),
                    Component.literal("Move to safety"), 0xFFFFC76D, 50, 60);
            HudAnnouncementS2CPacket.encode(announcement, buffer);
            helper.assertTrue(announcement.equals(HudAnnouncementS2CPacket.decode(buffer)), "Announcement content and priority round trip");
            buffer.clear();
            var guidance = new MatchGuidanceS2CPacket("arena", true, BattlezoneMap.MatchPhase.MATCH, 2, true, 119, 1200, true, java.util.List.of(600, 1200, 300, 1200));
            MatchGuidanceS2CPacket.encode(guidance, buffer);
            helper.assertTrue(guidance.equals(MatchGuidanceS2CPacket.decode(buffer)), "Guidance countdown and individual danger round trip");
        } finally { buffer.release(); }
        helper.succeed();
    }
}
