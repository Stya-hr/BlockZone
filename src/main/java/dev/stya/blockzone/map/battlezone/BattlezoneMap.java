package dev.stya.blockzone.map.battlezone;

import dev.stya.blockzone.net.battlezone.BattlezoneNetwork;
import dev.stya.blockzone.net.battlezone.ZoneStateS2CPacket;
import dev.stya.blockzone.util.battlezone.ZoneGeometry;
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
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.Optional;

public final class BattlezoneMap extends BaseMap {
    public static final String GAME_TYPE = "battlezone";
    private static final Logger LOGGER = LoggerFactory.getLogger(BattlezoneMap.class);

    private static final Codec<ZoneCenter> ZONE_CENTER_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.DOUBLE.fieldOf("x").forGetter(ZoneCenter::x),
            Codec.DOUBLE.fieldOf("z").forGetter(ZoneCenter::z)
    ).apply(instance, ZoneCenter::new));

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
    private final Setting<Double> deploymentSpeed;
    private final Setting<Double> deploymentHeight;
    private final DeploymentController deployment = new DeploymentController(this);
    private final LandingController landing = new LandingController();
    // Kept as settings so maps saved before configurable final centers can still use their old center.
    private final Setting<Double> poisonCenterX;
    private final Setting<Double> poisonCenterZ;
    private final Setting<List<ZoneCenter>> poisonFinalCenters;
    private final Setting<Integer> poisonDamage;
    private final Setting<List<PoisonPhase>> poisonPhases;
    private final Setting<String> boundaryTexture;

    private final java.util.Map<java.util.UUID, PlayerStateSnapshot> playerStates = new java.util.HashMap<>();
    private final SceneSnapshot sceneSnapshot;
    private MatchPhase phase = MatchPhase.WAITING;
    private int phaseTicks;
    private int poisonPhaseIndex;
    private int poisonPhaseTicks;
    private float poisonCurrentRadius;
    private double poisonCurrentCenterX;
    private double poisonCurrentCenterZ;
    private double poisonFinalCenterX;
    private double poisonFinalCenterZ;
    private double poisonStageStartCenterX;
    private double poisonStageStartCenterZ;
    private double poisonStageTargetCenterX;
    private double poisonStageTargetCenterZ;
    private float poisonStageStartRadius;
    private float poisonStageTargetRadius;
    private boolean poisonStageInitialized;
    private boolean snapshotValid;
    private boolean snapshotSavePending;
    private boolean victoryAnnounced;
    private long lastVisualStateSync = Long.MIN_VALUE;

    public BattlezoneMap(ServerLevel serverLevel, String mapName, AreaData areaData) {
        super(serverLevel, mapName, areaData);
        this.teamPlayerLimit = addSetting("battlezone", "team_player_limit", 3);
        this.totalPlayerLimit = addSetting("battlezone", "total_player_limit", 24);
        this.minimumTeamsToStart = addSetting("battlezone", "minimum_teams_to_start", 2);
        this.countdownSeconds = addSetting("battlezone", "countdown_seconds", 30);
        this.deploymentSeconds = addSetting("battlezone", "deployment_seconds", 15);
        this.settlementSeconds = addSetting("battlezone", "settlement_seconds", 10);
        this.deploymentSpeed = addSetting(new Setting<>("battlezone", "deployment_speed",
                Codec.doubleRange(0.1, 100.0), 20.0));
        this.deploymentHeight = addSetting(new Setting<>("battlezone", "deployment_height",
                Codec.doubleRange(-30_000_000.0, 1999.999),
                Math.min(1999.0, Math.max(areaData.pos1().getY(), areaData.pos2().getY()) + 64.0)));

        double defaultCenterX = (areaData.pos1().getX() + areaData.pos2().getX() + 1.0) / 2.0;
        double defaultCenterZ = (areaData.pos1().getZ() + areaData.pos2().getZ() + 1.0) / 2.0;
        this.poisonCenterX = addSetting("battlezone", "poison_center_x", defaultCenterX);
        this.poisonCenterZ = addSetting("battlezone", "poison_center_z", defaultCenterZ);
        this.poisonFinalCenters = addSetting(new Setting<>("battlezone", "poison_final_centers", ZONE_CENTER_CODEC.listOf(), List.of()));
        this.poisonDamage = addSetting("battlezone", "poison_damage_per_second", 1);
        this.poisonPhases = addSetting(new Setting<>("battlezone", "poison_phases", POISON_PHASE_CODEC.listOf(), DEFAULT_POISON_PHASES));
        this.boundaryTexture = addSetting("battlezone", "boundary_texture",
                "blockzone:textures/effect/battlezone_warning_fence.png");
        this.sceneSnapshot = new SceneSnapshot(this);
        this.snapshotValid = sceneSnapshot.load();
        if (!snapshotValid && !sceneSnapshot.exists()) {
            // New maps capture their initial baseline over several map ticks.
            sceneSnapshot.beginSave();
        }

        // Use the FPSMatch lobby timer for Battlezone's configured countdown.
        this.readyStartEnabled.set(false);
        this.autoStart.set(true);
        this.autoStartTime.set(Math.max(0, countdownSeconds.get()) * 20);
        ensureConfiguredTeams();
    }

    @Override
    public void tick() {
        if (!isStart) {
            ensureConfiguredTeams();
        }
        sceneSnapshot.tick();
        if (snapshotSavePending && !sceneSnapshot.isBusy()) {
            snapshotSavePending = false;
            if (sceneSnapshot.beginSave()) {
                snapshotValid = false;
            }
        }
        snapshotValid = sceneSnapshot.hasValidSnapshot();

        if (isMatchActive()) {
            deployment.tick();
        }
        switch (phase) {
            case WAITING -> tickWaiting();
            case COUNTDOWN -> tickCountdown();
            case DEPLOYMENT -> tickDeployment();
            case MATCH -> tickMatch();
            case SETTLEMENT -> tickSettlement();
            case RESETTING -> tickResetting();
        }
        syncVisualState(false);
    }

    private void tickWaiting() {
        autoStart.set(true);
        readyStartEnabled.set(false);
        // Restoration and snapshot capture both run across multiple ticks.
        // Keep the lobby waiting until the scene is ready for a match.
        if (isDebug() || sceneSnapshot.isBusy() || !hasMinimumTeams()) {
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
        if (sceneSnapshot.isBusy() || !hasMinimumTeams() || !hasValidSnapshot()) {
            phase = MatchPhase.WAITING;
            autoStartTime.set(Math.max(0, countdownSeconds.get()) * 20);
            broadcast(Component.literal("Battlezone countdown cancelled."));
        }
    }

    private void tickDeployment() {
        phaseTicks++;
        if (deployment.routeFinished() && phaseTicks >= Math.max(0, deploymentSeconds.get()) * 20) {
            phase = MatchPhase.MATCH;
            phaseTicks = 0;
            broadcast(Component.literal("Battlezone deployment completed."));
            syncVisualState(true);
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
            syncVisualState(true);
        }
    }

    private void tickPoisonZone() {
        List<PoisonPhase> phases = poisonPhases.get();
        if (poisonPhaseIndex >= phases.size()) {
            damagePlayersOutsideZone();
            return;
        }

        PoisonPhase stage = phases.get(poisonPhaseIndex);
        if (!poisonStageInitialized) {
            initializePoisonStage(phases);
        }
        int waitTicks = Math.max(0, stage.waitSeconds()) * 20;
        int shrinkTicks = Math.max(0, stage.shrinkSeconds()) * 20;
        poisonPhaseTicks++;

        if (poisonPhaseTicks <= waitTicks) {
            poisonCurrentRadius = poisonStageStartRadius;
            poisonCurrentCenterX = poisonStageStartCenterX;
            poisonCurrentCenterZ = poisonStageStartCenterZ;
        } else if (shrinkTicks == 0 || poisonPhaseTicks >= waitTicks + shrinkTicks) {
            poisonCurrentRadius = poisonStageTargetRadius;
            poisonCurrentCenterX = poisonStageTargetCenterX;
            poisonCurrentCenterZ = poisonStageTargetCenterZ;
            poisonPhaseIndex++;
            poisonPhaseTicks = 0;
            poisonStageInitialized = false;
        } else {
            float progress = (float) (poisonPhaseTicks - waitTicks) / shrinkTicks;
            poisonCurrentRadius = poisonStageStartRadius + (poisonStageTargetRadius - poisonStageStartRadius) * progress;
            poisonCurrentCenterX = poisonStageStartCenterX + (poisonStageTargetCenterX - poisonStageStartCenterX) * progress;
            poisonCurrentCenterZ = poisonStageStartCenterZ + (poisonStageTargetCenterZ - poisonStageStartCenterZ) * progress;
        }

        damagePlayersOutsideZone();
    }

    private void initializePoisonZone() {
        float initialRadius = getInitialPoisonRadius();
        poisonCurrentRadius = initialRadius;
        poisonCurrentCenterX = getMapCenterX();
        poisonCurrentCenterZ = getMapCenterZ();
        poisonStageInitialized = false;

        List<PoisonPhase> phases = poisonPhases.get();
        List<Float> targetRadii = getNormalizedTargetRadii(phases);
        float finalRadius = targetRadii.isEmpty() ? initialRadius : targetRadii.get(targetRadii.size() - 1);
        List<ZoneCenter> candidates = poisonFinalCenters.get();
        if (candidates.isEmpty()) {
            // Backward compatibility for maps that only configured poison_center_x/z.
            candidates = List.of(new ZoneCenter(poisonCenterX.get(), poisonCenterZ.get()));
        }

        float maximumOffset = Math.max(0.0F, initialRadius - finalRadius);
        List<ZoneCenter> validCenters = candidates.stream()
                .filter(center -> distanceSquared(center.x(), center.z(), poisonCurrentCenterX, poisonCurrentCenterZ)
                        <= (double) maximumOffset * maximumOffset)
                .toList();
        if (validCenters.size() < candidates.size()) {
            LOGGER.warn("Ignored {} invalid final poison center candidate(s) for Battlezone map {} because the final circle would not fit inside the starting zone",
                    candidates.size() - validCenters.size(), getMapName());
        }
        if (validCenters.isEmpty()) {
            // The map center is always a valid fallback, so every generated circle can contain the final circle.
            LOGGER.warn("No configured final poison center fits Battlezone map {}; using the map center", getMapName());
            poisonFinalCenterX = poisonCurrentCenterX;
            poisonFinalCenterZ = poisonCurrentCenterZ;
        } else {
            ZoneCenter chosen = validCenters.get(getServerLevel().getRandom().nextInt(validCenters.size()));
            poisonFinalCenterX = chosen.x();
            poisonFinalCenterZ = chosen.z();
        }
    }

    private void initializePoisonStage(List<PoisonPhase> phases) {
        List<Float> targetRadii = getNormalizedTargetRadii(phases);
        if (poisonPhaseIndex >= targetRadii.size()) {
            return;
        }

        poisonStageStartRadius = poisonCurrentRadius;
        poisonStageStartCenterX = poisonCurrentCenterX;
        poisonStageStartCenterZ = poisonCurrentCenterZ;
        poisonStageTargetRadius = targetRadii.get(poisonPhaseIndex);

        float finalRadius = targetRadii.isEmpty() ? poisonCurrentRadius : targetRadii.get(targetRadii.size() - 1);
        if (poisonPhaseIndex == phases.size() - 1) {
            poisonStageTargetCenterX = poisonFinalCenterX;
            poisonStageTargetCenterZ = poisonFinalCenterZ;
        } else {
            double maximumOffset = Math.max(0.0, poisonStageTargetRadius - finalRadius);
            RandomSource random = getServerLevel().getRandom();
            double angle = random.nextDouble() * Math.PI * 2.0;
            double distance = Math.sqrt(random.nextDouble()) * maximumOffset;
            poisonStageTargetCenterX = poisonFinalCenterX + Math.cos(angle) * distance;
            poisonStageTargetCenterZ = poisonFinalCenterZ + Math.sin(angle) * distance;
        }
        poisonStageInitialized = true;
    }

    private List<Float> getNormalizedTargetRadii(List<PoisonPhase> phases) {
        float initialRadius = getInitialPoisonRadius();
        float previousRadius = initialRadius;
        java.util.ArrayList<Float> targetRadii = new java.util.ArrayList<>(phases.size());
        for (PoisonPhase phase : phases) {
            float requestedRadius = initialRadius * Math.max(0.0F, Math.min(1.0F, phase.targetRadiusFraction()));
            previousRadius = Math.min(previousRadius, requestedRadius);
            targetRadii.add(previousRadius);
        }
        return targetRadii;
    }

    private static double distanceSquared(double x1, double z1, double x2, double z2) {
        double dx = x1 - x2;
        double dz = z1 - z2;
        return dx * dx + dz * dz;
    }

    private double getMapCenterX() {
        AreaData area = getMapArea();
        return (area.pos1().getX() + area.pos2().getX() + 1.0) / 2.0;
    }

    private double getMapCenterZ() {
        AreaData area = getMapArea();
        return (area.pos1().getZ() + area.pos2().getZ() + 1.0) / 2.0;
    }

    private void damagePlayersOutsideZone() {
        if (poisonDamage.get() <= 0 || getServerLevel().getGameTime() % 20 != 0) {
            return;
        }
        AreaData area = getMapArea();
        ZoneGeometry zone = new ZoneGeometry(poisonCurrentCenterX,
                ZoneGeometry.centerY(area.pos1().getY(), area.pos2().getY()),
                poisonCurrentCenterZ, poisonCurrentRadius);
        DamageSource damageSource = getServerLevel().damageSources().magic();
        for (ServerTeam team : getMapTeams().getNormalTeams()) {
            for (ServerPlayer player : team.getOnline()) {
                if (team.getPlayerData(player.getUUID()).map(data -> !data.isLiving()).orElse(true)) {
                    continue;
                }
                if (!hasDeploymentProtection(player) && !zone.contains(player.getX(), player.getY(), player.getZ())) {
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
        return !isDebug() && (phase == MatchPhase.WAITING || phase == MatchPhase.COUNTDOWN) && hasMinimumTeams() && hasValidSnapshot() && !sceneSnapshot.isBusy();
    }

    @Override
    protected boolean canReadyStart() {
        return false;
    }

    @Override
    public boolean start() {
        if (isStart || (!isDebug() && !hasMinimumTeams())) {
            return false;
        }
        if (phase != MatchPhase.WAITING && phase != MatchPhase.COUNTDOWN) {
            return false;
        }
        if (sceneSnapshot.isBusy()) {
            broadcast(Component.literal("Battlezone cannot start: scene restore is still in progress."));
            return false;
        }
        if (!snapshotValid) {
            broadcast(Component.literal("Battlezone cannot start: save a valid scene snapshot first."));
            return false;
        }
        var route = generateDeploymentRoute();
        if (route.isEmpty()) {
            broadcast(Component.literal("Battlezone cannot start: deployment_height must be at least 16 blocks above the map and below Y=2000, and the initial horizontal circle must have room for a route."));
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
        poisonPhaseIndex = 0;
        poisonPhaseTicks = 0;
        initializePoisonZone();
        deployment.start(route.get());
        broadcast(Component.literal("Battlezone match started."));
        syncVisualState(true);
        return true;
    }

    @Override
    public boolean allowJoinInProgress() {
        return isDebug() || phase == MatchPhase.DEPLOYMENT || phase == MatchPhase.MATCH;
    }

    @Override
    public MapTeams.JoinTeamResult join(ServerPlayer player) {
        ensureConfiguredTeams();
        PlayerStateSnapshot beforeJoin = captureBeforeJoin(player);
        if (isStart && !allowJoinInProgress()) {
            return MapTeams.JoinTeamResult.of(MapTeams.JoinTeamResult.Status.MID_MATCH_JOIN_DISABLED);
        }
        if (!checkGameHasPlayer(player) && getJoinedPlayerCount() >= Math.max(1, totalPlayerLimit.get())) {
            return MapTeams.JoinTeamResult.of(MapTeams.JoinTeamResult.Status.NO_AVAILABLE_TEAM);
        }
        if (!checkGameHasPlayer(player)) {
            return getMapTeams().getNormalTeams().stream()
                    .filter(team -> isDebug() || phase != MatchPhase.MATCH || !team.getLivingPlayers().isEmpty())
                    .filter(team -> team.getPlayerCount() < Math.max(1, teamPlayerLimit.get()))
                    .min(java.util.Comparator.comparingInt(ServerTeam::getPlayerCount))
                    .map(team -> joinConfiguredTeam(team.getName(), player, beforeJoin))
                    .orElseGet(() -> MapTeams.JoinTeamResult.of(MapTeams.JoinTeamResult.Status.NO_AVAILABLE_TEAM));
        }
        return getMapTeams().getTeamByPlayer(player)
                .map(team -> joinConfiguredTeam(team.getName(), player, beforeJoin))
                .orElseGet(() -> MapTeams.JoinTeamResult.of(MapTeams.JoinTeamResult.Status.NO_AVAILABLE_TEAM));
    }

    @Override
    public MapTeams.JoinTeamResult join(String teamName, ServerPlayer player) {
        ensureConfiguredTeams();
        PlayerStateSnapshot beforeJoin = captureBeforeJoin(player);
        if (isStart && !allowJoinInProgress()) {
            return MapTeams.JoinTeamResult.of(MapTeams.JoinTeamResult.Status.MID_MATCH_JOIN_DISABLED);
        }
        Optional<ServerTeam> team = getMapTeams().getTeamByName(teamName);
        boolean spectator = team.map(ServerTeam::isSpectator).orElse(false);
        if (!spectator && !checkGameHasPlayer(player) && getJoinedPlayerCount() >= Math.max(1, totalPlayerLimit.get())) {
            return MapTeams.JoinTeamResult.of(MapTeams.JoinTeamResult.Status.NO_AVAILABLE_TEAM);
        }
        if (!spectator && !isDebug() && phase == MatchPhase.MATCH && !checkGameHasPlayer(player)
                && team.map(value -> value.getLivingPlayers().isEmpty()).orElse(false)) {
            return MapTeams.JoinTeamResult.of(MapTeams.JoinTeamResult.Status.NO_AVAILABLE_TEAM);
        }
        boolean alreadyInTeam = team.map(value -> value.hasPlayer(player.getUUID())).orElse(false);
        if (!alreadyInTeam && team.isPresent() && !team.get().isSpectator() && team.get().getPlayerCount() >= Math.max(1, teamPlayerLimit.get())) {
            return MapTeams.JoinTeamResult.of(MapTeams.JoinTeamResult.Status.TEAM_FULL);
        }
        return joinConfiguredTeam(teamName, player, beforeJoin);
    }

    private PlayerStateSnapshot captureBeforeJoin(ServerPlayer player) {
        return com.ptcrys.fpsmatch.core.FPSMCore.getInstance().getMapByPlayerWithSpec(player)
                .filter(BattlezoneMap.class::isInstance)
                .map(BattlezoneMap.class::cast)
                .map(map -> map.playerStates.get(player.getUUID()))
                .orElseGet(() -> PlayerStateSnapshot.capture(player));
    }

    private MapTeams.JoinTeamResult joinConfiguredTeam(String teamName, ServerPlayer player, PlayerStateSnapshot beforeJoin) {
        // FPSMatch's join reads the setting directly instead of calling allowJoinInProgress().
        boolean previous = this.allowJoinInProgress.get();
        PlayerStateSnapshot original = playerStates.remove(player.getUUID());
        this.allowJoinInProgress.set(allowJoinInProgress());
        try {
            MapTeams.JoinTeamResult result = super.join(teamName, player);
            if (original != null) {
                if (result.isSuccess() || getMapTeams().getTeamByPlayer(player).isPresent()) {
                    playerStates.put(player.getUUID(), original);
                } else {
                    original.restore(player);
                }
            }
            return syncAfterJoin(result, player, original == null ? beforeJoin : original);
        } finally {
            this.allowJoinInProgress.set(previous);
        }
    }

    private MapTeams.JoinTeamResult syncAfterJoin(MapTeams.JoinTeamResult result, ServerPlayer player, PlayerStateSnapshot beforeJoin) {
        if (result.isSuccess()) {
            playerStates.putIfAbsent(player.getUUID(), beforeJoin);
        }
        if (result.isSuccess() && isStart) {
            BattlezoneNetwork.send(player, createVisualStatePacket());
            if (phase == MatchPhase.DEPLOYMENT) {
                deployment.board(player);
            }
        }
        return result;
    }

    @Override
    public ServerTeam addTeam(TeamData teamData) {
        TeamData limited = new TeamData(teamData.name(), Math.max(1, teamPlayerLimit.get()), teamData.capabilities());
        return super.addTeam(limited);
    }

    private void ensureConfiguredTeams() {
        // Keep the roster available before room selection and reconcile loaded settings.
        int maxPlayers = Math.max(1, totalPlayerLimit.get());
        int playersPerTeam = Math.max(1, teamPlayerLimit.get());
        int capacityTeamCount = (int) (((long) maxPlayers + playersPerTeam - 1) / playersPerTeam);
        int requiredTeams = Math.max(Math.max(1, minimumTeamsToStart.get()), capacityTeamCount);
        List<ServerTeam> teams = getMapTeams().getNormalTeams();
        for (ServerTeam team : List.copyOf(teams)) {
            if (!isStart && team.isEmpty() && team.getPlayerLimit() != playersPerTeam) {
                var capabilities = getMapTeams().getData();
                getMapTeams().delTeam(team.getPlayerTeam());
                addTeam(new TeamData(team.getName(), playersPerTeam));
                getMapTeams().writeData(capabilities);
            }
        }
        teams = getMapTeams().getNormalTeams();
        if (!isStart) {
            for (ServerTeam team : List.copyOf(teams)) {
                if (teams.size() <= requiredTeams) {
                    break;
                }
                if (team.isEmpty() && team.getName().startsWith("squad_")) {
                    getMapTeams().delTeam(team.getPlayerTeam());
                    teams = getMapTeams().getNormalTeams();
                }
            }
        }
        if (teams.size() >= requiredTeams) {
            return;
        }

        int nextNumber = 1;
        while (teams.size() < requiredTeams) {
            String teamName = "squad_" + nextNumber++;
            if (getMapTeams().getTeamByName(teamName).isPresent()) {
                continue;
            }
            addTeam(new TeamData(teamName, playersPerTeam));
            teams = getMapTeams().getNormalTeams();
        }
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
        syncVisualState(true);
    }

    @Override
    public boolean victoryGoal() {
        if (isDebug() || phase != MatchPhase.MATCH) {
            return false;
        }
        long teamsAlive = getMapTeams().getNormalTeams().stream()
                .filter(team -> !team.getLivingPlayers().isEmpty())
                .count();
        return teamsAlive <= 1;
    }

    @Override
    public boolean cleanupMap() {
        if (!super.cleanupMap()) {
            return false;
        }
        return hasValidSnapshot() && sceneSnapshot.beginRestore();
    }

    @Override
    public void reset() {
        deployment.clear();
        landing.clear();
        super.reset();
        isStart = false;
        syncVisualState(true);
        for (ServerPlayer player : List.copyOf(getMapTeams().getOnlineWithSpec())) {
            leave(player);
            // A cancelled leave event must not keep a finished match populated.
            if (getMapTeams().getTeamByPlayer(player).isPresent()) {
                getMapTeams().leaveTeam(player);
                PlayerStateSnapshot state = playerStates.remove(player.getUUID());
                if (state != null) {
                    state.restore(player);
                }
            }
        }
        for (java.util.UUID uuid : List.copyOf(getMapTeams().getJoinedPlayersWithSpec())) {
            getMapTeams().leaveTeam(uuid);
            PlayerStateSnapshot state = playerStates.remove(uuid);
            if (state != null) {
                PlayerStateSnapshot.restoreOnLogin(uuid, state);
            }
        }
        playerStates.forEach(PlayerStateSnapshot::restoreOnLogin);
        playerStates.clear();
        phase = MatchPhase.WAITING;
        phaseTicks = 0;
        poisonPhaseIndex = 0;
        poisonPhaseTicks = 0;
        poisonCurrentRadius = getInitialPoisonRadius();
        poisonCurrentCenterX = getMapCenterX();
        poisonCurrentCenterZ = getMapCenterZ();
        poisonStageInitialized = false;
        victoryAnnounced = false;
        getMapTeams().getNormalTeams().forEach(ServerTeam::resetLiving);
        if (!cleanupMap()) {
            phase = MatchPhase.WAITING;
            broadcast(Component.literal("Battlezone scene restore could not start; save a valid snapshot first."));
            syncVisualState(true);
            return;
        }
        phase = MatchPhase.RESETTING;
        syncVisualState(true);
    }

    @Override
    public void handlePlayerDisconnect(ServerPlayer player) {
        deployment.remove(player);
        landing.finish(player);
        super.handlePlayerDisconnect(player);
        if (getMapTeams().getTeamByPlayer(player).isEmpty()) {
            PlayerStateSnapshot state = playerStates.remove(player.getUUID());
            if (state != null) {
                PlayerStateSnapshot.restoreOnLogin(player.getUUID(), state);
            }
        }
    }

    @Override
    public void startNewRound() {
        if (isDebug() && isStart && (phase == MatchPhase.DEPLOYMENT || phase == MatchPhase.MATCH)) {
            boolean wasDeploying = phase == MatchPhase.DEPLOYMENT;
            phase = MatchPhase.MATCH;
            phaseTicks = 0;
            if (!wasDeploying) {
                poisonPhaseIndex = 0;
                poisonPhaseTicks = 0;
                initializePoisonZone();
            }
            syncVisualState(true);
        }
    }

    @Override
    public void leave(ServerPlayer player) {
        super.leave(player);
        if (getMapTeams().getTeamByPlayer(player).isEmpty()) {
            deployment.remove(player);
            landing.finish(player);
            BattlezoneNetwork.send(player, createVisualStatePacket(false));
            PlayerStateSnapshot state = playerStates.remove(player.getUUID());
            if (state != null) {
                state.restore(player);
            }
        }
    }

    @Override
    public void configFromJson(com.google.gson.JsonElement json) {
        super.configFromJson(json);
        if (!isStart) {
            ensureConfiguredTeams();
        }
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

    private java.util.Optional<FlightRoute> generateDeploymentRoute() {
        AreaData area = getMapArea();
        var zone = new ZoneGeometry(getMapCenterX(),
                ZoneGeometry.centerY(area.pos1().getY(), area.pos2().getY()),
                getMapCenterZ(), getInitialPoisonRadius());
        double highestY = Math.max(area.pos1().getY(), area.pos2().getY());
        var random = getServerLevel().getRandom();
        return FlightRoute.generateAcrossCircle(zone,
                Math.min(area.pos1().getX(), area.pos2().getX()) + 0.31,
                Math.max(area.pos1().getX(), area.pos2().getX()) + 0.69,
                Math.min(area.pos1().getZ(), area.pos2().getZ()) + 0.31,
                Math.max(area.pos1().getZ(), area.pos2().getZ()) + 0.69,
                highestY + 16, deploymentHeight.get(), deploymentSpeed.get(),
                random.nextDouble() * Math.PI * 2, random.nextDouble() * .06 - .03);
    }

    public boolean releaseDeployment(ServerPlayer player) {
        if (!deployment.protects(player)) {
            return false;
        }
        AreaData area = getMapArea();
        ZoneGeometry zone = new ZoneGeometry(poisonCurrentCenterX,
                ZoneGeometry.centerY(area.pos1().getY(), area.pos2().getY()),
                poisonCurrentCenterZ, poisonCurrentRadius);
        if (player.serverLevel() != getServerLevel()
                || !zone.containsHorizontal(player.getX(), player.getZ())) {
            player.displayClientMessage(Component.translatable("blockzone.deployment.outside_zone"), true);
            return false;
        }
        return deployment.release(player);
    }

    void clearAirbornePlayer(ServerPlayer player) {
        deployment.remove(player);
        landing.finish(player);
    }

    public boolean toggleParachute(ServerPlayer player) {
        return isMatchActive() && landing.toggleParachute(player);
    }

    public boolean hasDeploymentProtection(ServerPlayer player) {
        return deployment.protects(player) || landing.isDescending(player);
    }

    /** Shared entry point for route release and future airborne respawns. */
    public void beginLanding(ServerPlayer player) {
        if (isMatchActive() && getMapTeams().getTeamByPlayer(player)
                .filter(team -> !team.isSpectator()).flatMap(team -> team.getPlayerData(player.getUUID()))
                .map(data -> data.isLiving()).orElse(false)) {
            landing.begin(player);
        }
    }

    void tickDeploymentPlayer(ServerPlayer player) {
        deployment.tickPlayer(player);
        if (!isMatchActive() || player.serverLevel() != getServerLevel()
                || getMapTeams().getTeamByPlayer(player).filter(team -> !team.isSpectator())
                .flatMap(team -> team.getPlayerData(player.getUUID())).map(data -> !data.isLiving()).orElse(true)) {
            landing.finish(player);
        } else {
            landing.tick(player);
        }
    }

    public MatchPhase getPhase() {
        return phase;
    }

    public boolean isMatchActive() {
        return isStart && phase != MatchPhase.RESETTING;
    }

    private void syncVisualState(boolean force) {
        if (!force && !isStart) {
            return;
        }
        long gameTime = getServerLevel().getGameTime();
        if (!force && (lastVisualStateSync == Long.MIN_VALUE || gameTime - lastVisualStateSync < 5)) {
            return;
        }
        lastVisualStateSync = gameTime;
        ZoneStateS2CPacket packet = createVisualStatePacket();
        for (ServerPlayer player : getMapTeams().getOnlineWithSpec()) {
            BattlezoneNetwork.send(player, packet);
        }
    }

    private ZoneStateS2CPacket createVisualStatePacket() {
        return createVisualStatePacket(isStart && phase != MatchPhase.RESETTING);
    }

    private ZoneStateS2CPacket createVisualStatePacket(boolean boundaryVisible) {
        AreaData area = getMapArea();
        return new ZoneStateS2CPacket(
                getMapName(),
                getServerLevel().dimension().location(),
                boundaryVisible,
                boundaryVisible && (phase == MatchPhase.DEPLOYMENT || phase == MatchPhase.MATCH),
                area.pos1(),
                area.pos2(),
                poisonCurrentCenterX,
                poisonCurrentCenterZ,
                poisonCurrentRadius,
                boundaryTexture.get());
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

    public record ZoneCenter(double x, double z) {
    }

    public record PoisonPhase(int waitSeconds, int shrinkSeconds, float targetRadiusFraction) {
    }
}
