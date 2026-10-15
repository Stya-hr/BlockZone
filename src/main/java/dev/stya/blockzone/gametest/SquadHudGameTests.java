package dev.stya.blockzone.gametest;

import com.ptcrys.fpsmatch.core.data.AreaData;
import com.ptcrys.fpsmatch.core.data.PlayerData;
import dev.stya.blockzone.map.battlezone.BattlezoneMap;
import dev.stya.blockzone.net.battlezone.SquadStateS2CPacket;
import dev.stya.blockzone.squad.SquadStateSync;
import io.netty.buffer.Unpooled;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("blockzone")
@PrefixGameTestTemplate(false)
public final class SquadHudGameTests {
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void squadSnapshotsRespectTeamsOfflineRosterAndMatchScope(GameTestHelper helper) {
        var pos = helper.absolutePos(BlockPos.ZERO);
        var map = new BattlezoneMap(helper.getLevel(), "squad_" + UUID.randomUUID(), new AreaData(pos, pos.offset(2, 2, 2)));
        map.switchDebugMode();
        var teams = map.getMapTeams().getNormalTeams();
        var ally = UUID.randomUUID(); var enemy = UUID.randomUUID();
        teams.get(0).getPlayers().put(ally, new PlayerData(ally, Component.literal("Ally")));
        teams.get(1).getPlayers().put(enemy, new PlayerData(enemy, Component.literal("Enemy")));
        helper.assertTrue(SquadStateSync.capture(map, teams.get(0)).members().isEmpty(), "No vitals in waiting room");
        helper.succeedWhen(() -> {
            map.tick();
            helper.assertTrue(map.start(), "Wait for scene baseline");
            var packet = SquadStateSync.capture(map, teams.get(0));
            helper.assertTrue(packet.members().size() == 1 && packet.members().get(0).id().equals(ally), "Never include enemy vitals");
            helper.assertTrue(!packet.members().get(0).online(), "Offline allies remain represented");
            var buffer = new FriendlyByteBuf(Unpooled.buffer());
            try {
                SquadStateS2CPacket.encode(packet, buffer);
                helper.assertTrue(SquadStateS2CPacket.decode(buffer).equals(packet), "Network snapshot round trip");
            } finally { buffer.release(); }
            map.reset();
            helper.assertTrue(SquadStateSync.capture(map, teams.get(0)).members().isEmpty(), "Reset clears squad HUD scope");
            map.getCapabilityMap().clear();
        });
    }
    @GameTest(template = "empty")
    public static void firingFlagsSurviveSquadSnapshotTransport(GameTestHelper helper) {
        var member = new dev.stya.blockzone.squad.SquadMemberStatus(UUID.randomUUID(), "Shooter", 1,
                true, true, 100, 100, 50, 50, 2, 0, 0, 0, 0, 0, false, true);
        var packet = new SquadStateS2CPacket("map", "squad", java.util.List.of(member));
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            SquadStateS2CPacket.encode(packet, buffer);
            var decoded = SquadStateS2CPacket.decode(buffer);
            helper.assertTrue(decoded.equals(packet) && decoded.members().get(0).firing(), "Keep accepted firing flag and roster intact");
        } finally { buffer.release(); }
        helper.succeed();
    }

}
