package dev.stya.blockzone.map.battlezone.capability;

import com.ptcrys.fpsmatch.common.event.FPSMapEvent;
import com.ptcrys.fpsmatch.core.capability.FPSMCapability;
import com.ptcrys.fpsmatch.core.capability.FPSMCapabilityManager;
import com.ptcrys.fpsmatch.core.capability.map.MapCapability;
import com.ptcrys.fpsmatch.core.map.BaseMap;
import com.ptcrys.fpsmatch.core.match.RoundResult;
import dev.stya.blockzone.map.battlezone.BattlezoneMatchResult;
import dev.stya.blockzone.map.battlezone.BattlezoneResultReason;
import java.util.Optional;
import java.util.UUID;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Reads completed framework accounting; never maintains a second set of counters. */
public final class BattlezoneStatisticsCapability extends MapCapability {
    public record KillStatistics(UUID killer, UUID victim, boolean headshot, int kills, int assists, int headshotKills) { }
    private KillStatistics lastKill;
    private RoundResult<String, BattlezoneResultReason> outcome;
    private BattlezoneMatchResult result;

    public BattlezoneStatisticsCapability(BaseMap map) { super(map); }

    public static void register() {
        FPSMCapabilityManager.register(FPSMCapabilityManager.CapabilityType.MAP, BattlezoneStatisticsCapability.class,
                new FPSMCapability.Factory<BaseMap, BattlezoneStatisticsCapability>() {
                    @Override public BattlezoneStatisticsCapability create(BaseMap map) {
                        return new BattlezoneStatisticsCapability(map);
                    }
                });
    }

    public void begin() { lastKill = null; outcome = null; result = null; }
    public void prepareResult(RoundResult<String, BattlezoneResultReason> value) { outcome = value; }
    public Optional<KillStatistics> lastKill() { return Optional.ofNullable(lastKill); }
    public Optional<BattlezoneMatchResult> result() { return Optional.ofNullable(result); }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void killed(FPSMapEvent.PlayerEvent.KillEvent event) {
        if (event.getMap() != map || !map.isStart()) return;
        map.getMapTeams().getPlayerData(event.getPlayer()).ifPresent(data ->
                lastKill = new KillStatistics(event.getPlayer().getUUID(), event.getDead().getUUID(),
                        event.isHeadshot(), data.getKills(), data.getAssists(), data.getHeadshotKills()));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void victory(FPSMapEvent.VictoryEvent event) {
        if (event.getMap() != map || outcome == null || result != null) return;
        result = new BattlezoneMatchResult(map.getMapName(), outcome.winner(), outcome.reason(),
                map.getElapsedMatchTicks(), event.getScoreboard(), event.getTeamSummaries());
    }

    // Preserve the completed result for a result screen after the roster is removed.
    @Override public void reset() { lastKill = null; outcome = null; }
    @Override public void destroy() { reset(); result = null; }
}
