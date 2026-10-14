package dev.stya.blockzone.gametest;

import com.mojang.authlib.GameProfile;
import com.ptcrys.fpsmatch.core.data.AreaData;
import com.ptcrys.fpsmatch.core.map.DeathContext;
import dev.stya.blockzone.combat.BattlezoneDeathEvent;
import dev.stya.blockzone.combat.DownedController;
import dev.stya.blockzone.map.battlezone.BattlezoneMap;
import dev.stya.blockzone.map.battlezone.BattlezoneResultReason;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("blockzone")
@PrefixGameTestTemplate(false)
public final class StatisticsGameTests {
    /** GameTest actors have no socket/player-list entry; keep their score records online for survival rules. */
    private static final class OnlineScoreData extends com.ptcrys.fpsmatch.core.data.PlayerData {
        OnlineScoreData(ServerPlayer player) { super(player); }
        @Override public boolean isOnline() { return true; }
    }

    private static ServerPlayer player(GameTestHelper helper) {
        var id = UUID.randomUUID();
        var profile = new GameProfile(id, "stats_" + id.toString().substring(0, 8));
        // FakePlayer overrides die with a no-op. Use ServerPlayer's real death method with its no-op connection.
        var player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(), profile);
        player.connection = FakePlayerFactory.get(helper.getLevel(), profile).connection;
        player.setGameMode(GameType.ADVENTURE);
        return player;
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void delayedDeathUsesFrameworkAccountingAndSettlementSurvivesReset(GameTestHelper helper) {
        var pos = helper.absolutePos(BlockPos.ZERO);
        var map = new BattlezoneMap(helper.getLevel(), "stats_" + UUID.randomUUID(),
                new AreaData(pos, pos.offset(2, 2, 2)));
        map.switchDebugMode();
        var killer = player(helper);
        var victim = player(helper);
        var teams = map.getMapTeams().getNormalTeams();
        helper.assertTrue(map.join(teams.get(0).getName(), killer).isSuccess(), "Killer joins");
        helper.assertTrue(map.join(teams.get(1).getName(), victim).isSuccess(), "Victim joins another team");
        var killerData = new OnlineScoreData(killer);
        var victimData = new OnlineScoreData(victim);
        teams.get(0).getPlayers().put(killer.getUUID(), killerData);
        teams.get(1).getPlayers().put(victim.getUUID(), victimData);
        var started = new boolean[1];
        helper.succeedWhen(() -> {
            if (!started[0]) {
                map.tick();
                helper.assertTrue(map.start(), "Wait for valid scene snapshot");
                map.startNewRound();
                started[0] = true;
                var source = helper.getLevel().damageSources().playerAttack(killer);
                map.recordHurtData(victim, source, 10);
                map.creditAssist(killerData);
                var context = new DeathContext(victim, killer, source, ItemStack.EMPTY,
                        helper.getLevel().getGameTime());
                context.setGunKill(true);
                context.setHeadShot(true);
                DownedController.finish(context, BattlezoneDeathEvent.Reason.BLEED_OUT);
            }
            helper.assertTrue(map.statistics().lastKill().isPresent(), "Wait for framework death finalization");
            var kill = map.statistics().lastKill().orElseThrow();
            helper.assertTrue(kill.kills() == 1 && killerData.getKills() == 1, "Kill counted exactly once by FPSMatch");
            helper.assertTrue(kill.headshotKills() == 1 && kill.headshot(), "Delayed headshot reaches completed accounting");
            helper.assertTrue(victimData.getDeaths() == 1 && !victimData.isLiving(), "Framework records elimination once");
            helper.assertTrue(killerData.getDamage() == 10, "No artificial finishing damage enters statistics: " + killerData.getDamage());
            helper.assertTrue(kill.assists() == 1, "Read completed framework assist count");
            map.victory();
            var result = map.getMatchResult().orElseThrow();
            helper.assertTrue(result.reason() == BattlezoneResultReason.LAST_TEAM_STANDING
                    && result.winner().equals(teams.get(0).getName()), "Winner resolved by survival rule");
            helper.assertTrue(result.players().get(killer.getUUID()).kills() == 1
                    && result.players().get(killer.getUUID()).assists() == 1
                    && result.players().get(victim.getUUID()).deaths() == 1, "Settlement uses framework score snapshots");
            helper.assertTrue(result.teams().get(teams.get(0).getName()).totalKills() == 1, "Team settlement uses framework summary");
            map.victory();
            helper.assertTrue(map.getMatchResult().orElseThrow() == result, "Repeated victory preserves single settlement");
            killerData.setKills(99);
            helper.assertTrue(result.players().get(killer.getUUID()).kills() == 1, "Settlement does not reference mutable player counters");
            map.reset();
            helper.assertTrue(map.getMatchResult().orElseThrow() == result, "Result survives roster removal and reset");
            map.statistics().begin();
            helper.assertTrue(map.getMatchResult().isEmpty(), "Next match clears previous settlement");
            map.getCapabilityMap().clear();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void drawSettlementNeedsNoKillerAndDestroyReleasesResult(GameTestHelper helper) {
        var pos = helper.absolutePos(BlockPos.ZERO);
        var map = new BattlezoneMap(helper.getLevel(), "draw_" + UUID.randomUUID(),
                new AreaData(pos, pos.offset(2, 2, 2)));
        map.switchDebugMode();
        helper.succeedWhen(() -> {
            map.tick();
            helper.assertTrue(map.start(), "Wait for valid scene snapshot");
            map.startNewRound();
            map.victory();
            var result = map.getMatchResult().orElseThrow();
            helper.assertTrue(result.reason() == BattlezoneResultReason.DRAW && result.winner() == null,
                    "No surviving team produces a draw");
            helper.assertTrue(result.players().isEmpty() && map.statistics().lastKill().isEmpty(),
                    "Settlement requires no player killer");
            var statistics = map.statistics();
            map.getCapabilityMap().clear();
            helper.assertTrue(statistics.result().isEmpty(), "Capability destruction releases settlement");
        });
    }
}
