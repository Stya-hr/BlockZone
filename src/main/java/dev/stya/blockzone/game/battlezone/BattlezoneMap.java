package dev.stya.blockzone.game.battlezone;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.ptcrys.fpsmatch.core.data.AreaData;
import com.ptcrys.fpsmatch.core.data.Setting;
import com.ptcrys.fpsmatch.core.map.BaseMap;
import com.ptcrys.fpsmatch.core.team.MapTeams;
import com.ptcrys.fpsmatch.core.team.ServerTeam;
import com.ptcrys.fpsmatch.core.team.TeamData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;

import java.util.List;
import java.util.Optional;

public final class BattlezoneMap extends BaseMap {
    public static final String GAME_TYPE = "battlezone";

    private static final Codec<PoisonPhase> POISON_PHASE_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.intRange(0, Integer.MAX_VALUE).fieldOf("wait_seconds").forGetter(PoisonPhase::waitSeconds),
            Codec.intRange(0, Integer.MAX_VALUE).fieldOf("shrink_seconds").forGetter(PoisonPhase::shrinkSeconds),
            Codec.floatRange(0.0F, 1.0F).fieldOf("target_radius_fraction").forGetter(PoisonPhase::targetRadiusFraction)
    ).apply(instance, PoisonPhase::new));

    private static final List<PoisonPhase> DEFAULT_POISON_PHASES = List.of(
            new PoisonPhase(30, 60, 0.80F),
            new PoisonPhase(30, 60, 0.55F),
            new PoisonPhase(30, 60, 0.25F),
            new PoisonPhase(30, 60, 0.0F)
    );

    private final Setting<Integer> teamPlayerLimit;
    private final Setting<Integer> totalPlayerLimit;
    private final Setting<Integer> minimumTeamsToStart;
    private final Setting<Integer> countdownSeconds;
    private final Setting<Integer> deploymentSeconds;
    private final Setting<Integer> settlementSeconds;
    private final Setting<Double> poisonCenterX;
    private final Setting<Double> poisonCenterZ;
    private final Setting<Integer> poisonDamage;
    private final Setting<List<PoisonPhase>> poisonPhases;

    private final BattlezoneSceneSnapshot sceneSnapshot;
    private MatchPhase phase = MatchPhase.WAITING;
    private int phaseTicks;
    private int poisonPhaseIndex;
    private int poisonPhaseTicks;
    private float poisonStartRadius;
    private float poisonCurrentRadius;
    private boolean snapshotValid;
    private boolean snapshotSavePending;
    private boolean victoryAnnounced;

    public BattlezoneMap(ServerLevel serverLevel, String mapName, AreaData areaData) {
        super(serverLevel, mapName, areaData);
        this.teamPlayerLimit = addSetting("battlezone", "team_player_limit", 3);
        this.totalPlayerLimit = addSetting("battlezone", "total_player_limit", 24);
        this.minimumTeamsToStart = addSetting("battlezone", "minimum_teams_to_start", 2);
        this.countdownSeconds = addSetting("battlezone", "countdown_seconds", 30);
        this.deploymentSeconds = addSetting("battlezone", "deployment_seconds", 15);
        this.settlementSeconds = addSetting("battlezone", "settlement_seconds", 10);

        double defaultCenterX = (areaData.pos1().getX() + areaData.pos2().getX() + 1.0) / 2.0;
        double defaultCenterZ = (areaData.pos1().getZ() + areaData.pos2().getZ() + 1.0) / 2.0;
        this.poisonCenterX = addSetting("battlezone", "poison_center_x", defaultCenterX);
        this.poisonCenterZ = addSetting("battlezone", "poison_center_z", defaultCenterZ);
        this.poisonDamage = addSetting("battlezone", "poison_damage_per_second", 1);
        this.poisonPhases = addSetting(new Setting<>("battlezone", "poison_phases", POISON_PHASE_CODEC.listOf(), DEFAULT_POISON_PHASES));
        this.sceneSnapshot = new BattlezoneSceneSnapshot(this);
        this.snapshotValid = sceneSnapshot.load();
        if (!snapshotValid && !sceneSnapshot.exists()) {
            // New maps capture their initial baseline over several map ticks.
            sceneSnapshot.beginSave();
        }

        // Use the FPSMatch lobby timer for Battlezone's configured countdown.
        this.readyStartEnabled.set(false);
        this.autoStart.set(true);
        this.autoStartTime.set(Math.max(0, countdownSeconds.get()) * 20);
    }

    @Override
    public void tick() {
        sceneSnapshot.tick();
        if (snapshotSavePending && !sceneSnapshot.isBusy()) {
            snapshotSavePending = false;
            if (sceneSnapshot.beginSave()) {
                snapshotValid = false;
            }
        }
        snapshotValid = sceneSnapshot.hasValidSnapshot();

        switch (phase) {
            case WAITING -> tickWaiting();
            case COUNTDOWN -> tickCountdown();
            case DEPLOYMENT -> tickDeployment();
            case MATCH -> tickMatch();
            case SETTLEMENT -> tickSettlement();
            case RESETTING -> tickResetting();
        }
    }

    private void tickWaiting() {
        autoStart.set(true);
        readyStartEnabled.set(false);
        if (!hasMinimumTeams()) {
            return;
        }
        if (!hasValidSnapshot()) {
            return;
        }
        phase = MatchPhase.COUNTDOWN;
        autoStartTime.set(Math.max(0, countdownSeconds.get()) * 20);
        broadcast(Component.literal("Battlezone starts in " + countdownSeconds.get() + " seconds."));
    }

    private void tickCountdown() {
        autoStart.set(true);
        readyStartEnabled.set(false);
        if (!hasMinimumTeams() || !hasValidSnapshot()) {
            phase = MatchPhase.WAITING;
            broadcast(Component.literal("Battlezone countdown cancelled."));
        }
        autoStartTime.set(Math.max(0, countdownSeconds.get()) * 20);
    }

    private void tickDeployment() {
        phaseTicks++;
        if (phaseTicks >= Math.max(0, deploymentSeconds.get()) * 20) {
            phase = MatchPhase.MATCH;
            phaseTicks = 0;
            poisonPhaseIndex = 0;
            poisonPhaseTicks = 0;
            poisonStartRadius = getInitialPoisonRadius();
            poisonCurrentRadius = poisonStartRadius;
            broadcast(Component.literal("Battlezone match started."));
        }
    }

    private void tickMatch() {
        tickPoisonZone();
    }

    private void tickSettlement() {
        phaseTicks++;
        if (phaseTicks >= Math.max(0, settlementSeconds.get()) * 20) {
            reset();
        }
    }

    private void tickResetting() {
        if (!sceneSnapshot.isBusy()) {
            if (!sceneSnapshot.lastOperationSucceeded()) {
                broadcast(Component.literal("Battlezone scene restore failed; save a new snapshot before the next match."));
            }
            phase = MatchPhase.WAITING;
            phaseTicks = 0;
        }
    }

    private void tickPoisonZone() {
        List<PoisonPhase> phases = poisonPhases.get();
        if (poisonPhaseIndex >= phases.size()) {
            damagePlayersOutsideZone();
            return;
        }

        PoisonPhase stage = phases.get(poisonPhaseIndex);
        int waitTicks = Math.max(0, stage.waitSeconds()) * 20;
        int shrinkTicks = Math.max(0, stage.shrinkSeconds()) * 20;
        poisonPhaseTicks++;

        if (poisonPhaseTicks <= waitTicks) {
            poisonCurrentRadius = poisonStartRadius;
        } else if (shrinkTicks == 0 || poisonPhaseTicks >= waitTicks + shrinkTicks) {
            poisonCurrentRadius = poisonStartRadius * stage.targetRadiusFraction();
            poisonPhaseIndex++;
            poisonPhaseTicks = 0;
            poisonStartRadius = poisonCurrentRadius;
        } else {
            float progress = (float) (poisonPhaseTicks - waitTicks) / shrinkTicks;
            float target = getInitialPoisonRadius() * stage.targetRadiusFraction();
            poisonCurrentRadius = poisonStartRadius + (target - poisonStartRadius) * progress;
        }

        damagePlayersOutsideZone();
    }

    private void damagePlayersOutsideZone() {
        if (poisonDamage.get() <= 0 || getServerLevel().getGameTime() % 20 != 0) {
            return;
        }
        double centerX = poisonCenterX.get();
        double centerZ = poisonCenterZ.get();
        double radiusSquared = (double) poisonCurrentRadius * poisonCurrentRadius;
        DamageSource damageSource = getServerLevel().damageSources().magic();
        for (ServerTeam team : getMapTeams().getNormalTeams()) {
            for (ServerPlayer player : team.getOnline()) {
                if (team.getPlayerData(player.getUUID()).map(data -> !data.isLiving()).orElse(true)) {
                    continue;
                }
                double dx = player.getX() - centerX;
                double dz = player.getZ() - centerZ;
                if (dx * dx + dz * dz > radiusSquared) {
                    player.hurt(damageSource, poisonDamage.get());
                }
            }
        }
    }

    private float getInitialPoisonRadius() {
        AreaData area = getMapArea();
        if (area == null) {
            return 0.0F;
        }
        int width = Math.abs(area.pos1().getX() - area.pos2().getX()) + 1;
        int depth = Math.abs(area.pos1().getZ() - area.pos2().getZ()) + 1;
        return Math.min(width, depth) / 2.0F;
    }

    private boolean hasMinimumTeams() {
        int occupiedTeams = 0;
        for (ServerTeam team : getMapTeams().getNormalTeams()) {
            if (!team.isEmpty()) {
                occupiedTeams++;
            }
        }
        return occupiedTeams >= Math.max(1, minimumTeamsToStart.get());
    }

    private int getJoinedPlayerCount() {
        return getMapTeams().getNormalTeams().stream().mapToInt(ServerTeam::getPlayerCount).sum();
    }

    private boolean hasValidSnapshot() {
        return snapshotValid;
    }

    private void broadcast(Component message) {
        for (ServerPlayer player : getMapTeams().getOnlineWithSpec()) {
            player.sendSystemMessage(message);
        }
    }

    @Override
    protected boolean canAutoStart() {
        return hasMinimumTeams() && hasValidSnapshot() && !sceneSnapshot.isBusy();
    }

    @Override
    protected boolean canReadyStart() {
        return false;
    }

    @Override
    public boolean start() {
        if (isStart || !hasMinimumTeams()) {
            return false;
        }
        if (!snapshotValid) {
            broadcast(Component.literal("Battlezone cannot start: save a valid scene snapshot first."));
            return false;
        }
        if (!super.start()) {
            return false;
        }
        isStart = true;
        phase = MatchPhase.DEPLOYMENT;
        phaseTicks = 0;
        victoryAnnounced = false;
        resetMatchClock();
        return true;
    }

    @Override
    public boolean allowJoinInProgress() {
        return phase == MatchPhase.DEPLOYMENT || phase == MatchPhase.MATCH;
    }

    @Override
    public MapTeams.JoinTeamResult join(ServerPlayer player) {
        if (isStart && !allowJoinInProgress()) {
            return MapTeams.JoinTeamResult.of(MapTeams.JoinTeamResult.Status.MID_MATCH_JOIN_DISABLED);
        }
        if (!checkGameHasPlayer(player) && getJoinedPlayerCount() >= Math.max(1, totalPlayerLimit.get())) {
            return MapTeams.JoinTeamResult.of(MapTeams.JoinTeamResult.Status.NO_AVAILABLE_TEAM);
        }
        if (!checkGameHasPlayer(player)) {
            return getMapTeams().getNormalTeams().stream()
                    .filter(team -> phase != MatchPhase.MATCH || !team.getLivingPlayers().isEmpty())
                    .filter(team -> team.getPlayerCount() < Math.max(1, teamPlayerLimit.get()))
                    .min(java.util.Comparator.comparingInt(ServerTeam::getPlayerCount))
                    .map(team -> super.join(team.getName(), player))
                    .orElseGet(() -> MapTeams.JoinTeamResult.of(MapTeams.JoinTeamResult.Status.NO_AVAILABLE_TEAM));
        }
        return super.join(player);
    }

    @Override
    public MapTeams.JoinTeamResult join(String teamName, ServerPlayer player) {
        if (isStart && !allowJoinInProgress()) {
            return MapTeams.JoinTeamResult.of(MapTeams.JoinTeamResult.Status.MID_MATCH_JOIN_DISABLED);
        }
        if (!checkGameHasPlayer(player) && getJoinedPlayerCount() >= Math.max(1, totalPlayerLimit.get())) {
            return MapTeams.JoinTeamResult.of(MapTeams.JoinTeamResult.Status.NO_AVAILABLE_TEAM);
        }
        Optional<ServerTeam> team = getMapTeams().getTeamByName(teamName);
        if (phase == MatchPhase.MATCH && !checkGameHasPlayer(player)
                && team.map(value -> value.getLivingPlayers().isEmpty()).orElse(false)) {
            return MapTeams.JoinTeamResult.of(MapTeams.JoinTeamResult.Status.NO_AVAILABLE_TEAM);
        }
        boolean alreadyInTeam = team.map(value -> value.hasPlayer(player.getUUID())).orElse(false);
        if (!alreadyInTeam && team.isPresent() && team.get().getPlayerCount() >= Math.max(1, teamPlayerLimit.get())) {
            return MapTeams.JoinTeamResult.of(MapTeams.JoinTeamResult.Status.TEAM_FULL);
        }
        return super.join(teamName, player);
    }

    @Override
    public ServerTeam addTeam(TeamData teamData) {
        TeamData limited = new TeamData(teamData.name(), Math.max(1, teamPlayerLimit.get()), teamData.capabilities());
        return super.addTeam(limited);
    }

    @Override
    public void victory() {
        if (phase != MatchPhase.MATCH || victoryAnnounced) {
            return;
        }
        victoryAnnounced = true;
        List<ServerTeam> livingTeams = getMapTeams().getNormalTeams().stream()
                .filter(team -> !team.getLivingPlayers().isEmpty())
                .toList();
        if (livingTeams.isEmpty()) {
            broadcast(Component.literal("Battlezone ended in a draw."));
        } else if (livingTeams.size() == 1) {
            livingTeams.get(0).sendMessage(Component.literal("Your team won the Battlezone."));
            for (ServerTeam team : getMapTeams().getNormalTeams()) {
                if (team != livingTeams.get(0)) {
                    team.sendMessage(Component.literal("Your team was eliminated from the Battlezone."));
                }
            }
        }
        super.victory();
        phase = MatchPhase.SETTLEMENT;
        phaseTicks = 0;
    }

    @Override
    public boolean victoryGoal() {
        if (phase != MatchPhase.MATCH) {
            return false;
        }
        long teamsAlive = getMapTeams().getNormalTeams().stream()
                .filter(team -> !team.getLivingPlayers().isEmpty())
                .count();
        return teamsAlive <= 1;
    }

    @Override
    public void reset() {
        super.reset();
        isStart = false;
        phase = MatchPhase.WAITING;
        phaseTicks = 0;
        poisonPhaseIndex = 0;
        poisonPhaseTicks = 0;
        poisonStartRadius = getInitialPoisonRadius();
        poisonCurrentRadius = poisonStartRadius;
        victoryAnnounced = false;
        getMapTeams().getNormalTeams().forEach(ServerTeam::resetLiving);
        if (!hasValidSnapshot() || !sceneSnapshot.beginRestore()) {
            phase = MatchPhase.WAITING;
            broadcast(Component.literal("Battlezone scene restore could not start; save a valid snapshot first."));
            return;
        }
        phase = MatchPhase.RESETTING;
    }

    @Override
    public void setMapArea(AreaData areaData) {
        super.setMapArea(areaData);
        if (sceneSnapshot != null && !isStart && areaData != null) {
            snapshotValid = false;
            if (sceneSnapshot.isBusy()) {
                snapshotSavePending = true;
            } else {
                sceneSnapshot.beginSave();
            }
        }
    }

    public boolean saveSceneSnapshot() {
        boolean started = sceneSnapshot.beginSave();
        if (started) {
            snapshotValid = false;
        }
        return started;
    }

    public MatchPhase getPhase() {
        return phase;
    }

    @Override
    public String getGameType() {
        return GAME_TYPE;
    }

    public enum MatchPhase {
        WAITING,
        COUNTDOWN,
        DEPLOYMENT,
        MATCH,
        SETTLEMENT,
        RESETTING
    }

    public record PoisonPhase(int waitSeconds, int shrinkSeconds, float targetRadiusFraction) {
    }
}
