package dev.stya.blockzone.gametest;

import com.ptcrys.fpsmatch.core.data.AreaData;
import com.ptcrys.fpsmatch.core.data.PlayerData;
import dev.stya.blockzone.chat.*;
import dev.stya.blockzone.map.battlezone.BattlezoneMap;
import dev.stya.blockzone.net.battlezone.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.gametest.*;
import java.util.UUID;

@GameTestHolder("blockzone")
@PrefixGameTestTemplate(false)
public final class MatchChatGameTests {
    @GameTest(template = "empty")
    public static void chatRecipientsRespectMapTeamsAndSpectators(GameTestHelper helper) {
        var pos = helper.absolutePos(BlockPos.ZERO);
        var map = new BattlezoneMap(helper.getLevel(), "chat_" + UUID.randomUUID(), new AreaData(pos, pos.offset(2, 2, 2)));
        var teams = map.getMapTeams().getNormalTeams();
        var sender = UUID.randomUUID(); var ally = UUID.randomUUID(); var enemy = UUID.randomUUID();
        var spectator = UUID.randomUUID(); var outsider = UUID.randomUUID();
        teams.get(0).getPlayers().put(sender, new PlayerData(sender, Component.literal("Sender")));
        teams.get(0).getPlayers().put(ally, new PlayerData(ally, Component.literal("Ally")));
        teams.get(1).getPlayers().put(enemy, new PlayerData(enemy, Component.literal("Enemy")));
        map.getMapTeams().getTeamsWithSpectator().stream().filter(t -> t.isSpectator()).findFirst().orElseThrow()
                .getPlayers().put(spectator, new PlayerData(spectator, Component.literal("Spectator")));
        helper.assertTrue(MatchChat.canReceive(map, sender, ally, ChatChannel.SQUAD), "Ally receives squad chat");
        helper.assertTrue(MatchChat.canReceive(map, sender, sender, ChatChannel.SQUAD), "Sender receives own chat");
        helper.assertTrue(!MatchChat.canReceive(map, sender, enemy, ChatChannel.SQUAD), "Enemy cannot receive squad chat");
        helper.assertTrue(!MatchChat.canReceive(map, sender, spectator, ChatChannel.SQUAD), "Spectator cannot receive squad chat");
        helper.assertTrue(!MatchChat.canReceive(map, spectator, spectator, ChatChannel.SQUAD), "Spectator cannot send squad chat");
        helper.assertTrue(MatchChat.canReceive(map, sender, enemy, ChatChannel.ALL), "Enemy receives map chat");
        helper.assertTrue(MatchChat.canReceive(map, sender, spectator, ChatChannel.ALL), "Spectator receives map chat");
        helper.assertTrue(!MatchChat.canReceive(map, sender, outsider, ChatChannel.ALL), "Other-map player cannot receive map chat");
        helper.assertTrue(!MatchChat.canReceive(map, outsider, sender, ChatChannel.ALL), "Non-member cannot send map chat");
        var buffer = new FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        try {
            var packet = new MatchChatS2CPacket(map.getMapName(), ChatChannel.SQUAD, sender, "Sender", "中文 hello");
            MatchChatS2CPacket.encode(packet, buffer);
            helper.assertTrue(packet.equals(MatchChatS2CPacket.decode(buffer)), "Scoped channel payload round trip");
        } finally { buffer.release(); }
        map.getCapabilityMap().clear(); helper.succeed();
    }
}
