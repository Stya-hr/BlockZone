package dev.stya.blockzone.map.battlezone;

import dev.stya.blockzone.net.battlezone.BattlezoneNetwork;
import dev.stya.blockzone.net.battlezone.ZoneStateS2CPacket;
import dev.stya.blockzone.util.battlezone.ZoneGeometry;
import dev.stya.blockzone.util.battlezone.PoisonPath;
import dev.stya.blockzone.util.battlezone.BoundaryGeometry;
import com.mojang.serialization.Codec;
import dev.stya.blockzone.util.CodecSettings;
import dev.stya.blockzone.util.battlezone.PoisonSettingsMigration;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.Optional;

public final class BattlezoneMap extends BaseMap {
    public static final String GAME_TYPE = "battlezone";
    private static final Logger LOGGER = LoggerFactory.getLogger(BattlezoneMap.class);

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
    private final Setting<List<dev.stya.blockzone.equipment.StartingLoadout.Entry>> startingLoadout;
    private List<dev.stya.blockzone.equipment.StartingLoadout.Prepared> activeLoadout = List.of();
    private final Setting<Double> matchHealth;
    private final Setting<Double> armorPlatePoints;
    private float activeMatchHealth = CombatRecovery.MAX_HEALTH;
    private float activePlatePoints = CombatRecovery.PLATE_POINTS;

    public float getCombatHealth() { return activeMatchHealth; }
    public float getArmorPlatePoints() { return activePlatePoints; }
    public float getMaxCombatArmor(ServerPlayer player) { return activePlatePoints * dev.stya.blockzone.equipment.PlateCapacity.slots(player); }

    private final Setting<Double> poisonDamage;
    private final Setting<List<PoisonPath>> poisonSequences;
    private List<PoisonPath.Circle> activePoisonPhases = List.of();
    private PoisonPath activePoisonPath;
    private double activeBaseDamage;
    private int activeSequenceNumber;
    private final Setting<String> boundaryTexture;

    private final java.util.Map<java.util.UUID, PlayerStateSnapshot> playerStates = new java.util.HashMap<>();
    private final java.util.Map<java.util.UUID, CombatRecovery> recovery = new java.util.HashMap<>();

    public void combatHurt(ServerPlayer player) {
        recovery.computeIfAbsent(player.getUUID(), id -> new CombatRecovery()).hurt();
        if (player.getUseItem().is(dev.stya.blockzone.loot.LootCrateRegistry.ARMOR_PLATE.get())) {
            player.stopUsingItem();
        }
    }

    public void tickRecovery(ServerPlayer player) {
        if (MatchRegeneration.allowed(player)) return;
        if (recovery.computeIfAbsent(player.getUUID(), id -> new CombatRecovery()).tick()
                && player.isAlive() && player.getHealth() < player.getMaxHealth()) {
            player.heal(activeMatchHealth * 0.05F);
        }
    }

    private void clearCombat(ServerPlayer player) {
        recovery.remove(player.getUUID());
        player.stopUsingItem();
        player.setAbsorptionAmount(0);
    }
    private final SceneSnapshot sceneSnapshot;
    private MatchPhase phase = MatchPhase.WAITING;
    private int phaseTicks;
    private int poisonPhaseIndex;
    private int poisonPhaseTicks;
    private float poisonCurrentRadius;
    private double poisonCurrentCenterX;
    private double poisonCurrentCenterZ;
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
    private java.util.UUID lootRoundId;
    private long lastVisualStateSync = Long.MIN_VALUE;

    public BattlezoneMap(ServerLevel serverLevel, String mapName, AreaData areaData) {
        super(serverLevel, mapName, areaData);
        this.teamPlayerLimit = addSetting("battlezone", "teamPlayerLimit", 3);
        this.totalPlayerLimit = addSetting("battlezone", "totalPlayerLimit", 24);
        this.minimumTeamsToStart = addSetting("battlezone", "minimumTeamsToStart", 2);
        this.countdownSeconds = addSetting("battlezone", "countdownSeconds", 30);
        this.deploymentSeconds = addSetting("battlezone", "deploymentSeconds", 15);
        this.settlementSeconds = addSetting("battlezone", "settlementSeconds", 10);
        this.deploymentSpeed = addSetting(CodecSettings.create("battlezone", "deploymentSpeed",
                Codec.doubleRange(0.1, 100.0), 20.0));
        this.deploymentHeight = addSetting(CodecSettings.create("battlezone", "deploymentHeight",
                CodecSettings.FINITE_DOUBLE,
                Math.max(areaData.pos1().getY(), areaData.pos2().getY()) + 64.0));

        this.startingLoadout = addSetting(CodecSettings.create("battlezone", "startingLoadout",
                dev.stya.blockzone.equipment.StartingLoadout.Entry.CODEC.listOf(), dev.stya.blockzone.equipment.StartingLoadout.defaults()));
        this.matchHealth = addSetting(CodecSettings.create("battlezone", "matchHealth",
                Codec.doubleRange(1.0, 1024.0), 100.0));
        this.armorPlatePoints = addSetting(CodecSettings.create("battlezone", "armorPlatePoints",
                Codec.doubleRange(1.0, 1024.0), 50.0));
        this.poisonDamage = addSetting(CodecSettings.create("battlezone", "poisonDamagePerSecond",
                CodecSettings.NONNEGATIVE_DOUBLE, 1.0));
        this.poisonSequences = addSetting(CodecSettings.create("battlezone", "poisonSequences", PoisonPath.CODEC.listOf(),
                List.of(PoisonSettingsMigration.defaults(getMapCenterX(), getMapCenterZ(), getInitialPoisonRadius()))));
        this.boundaryTexture = addSetting("battlezone", "boundaryTexture",
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
        List<PoisonPath.Circle> phases = activePoisonPhases;
        if (poisonPhaseIndex >= phases.size()) {
            damagePlayersOutsideZone();
            return;
        }

        PoisonPath.Circle stage = phases.get(poisonPhaseIndex);
        if (!poisonStageInitialized) {
            initializePoisonStage();
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

    private List<ZoneGeometry> poisonSequence = List.of();
    private List<ZoneGeometry> resolvePath(PoisonPath path, int number) {
        var area = getMapArea();
        var bounds = BoundaryGeometry.of(area.pos1().getX(), area.pos1().getZ(), area.pos2().getX(), area.pos2().getZ());
        var result = path.resolve(bounds, ZoneGeometry.centerY(area.pos1().getY(), area.pos2().getY()));
        for (int i = 0; i < result.size(); i++) {
            var configured = path.circles().get(i);
            var actual = result.get(i);
            if (configured.x() != actual.centerX() || configured.z() != actual.centerZ()) {
                var warning = "Battlezone " + getMapName() + " sequence " + number + " circle " + i
                        + " exceeds map bounds; runtime center=(" + actual.centerX() + ", " + actual.centerZ()
                        + "). Radius and saved configuration unchanged.";
                LOGGER.warn(warning);
                getServerLevel().players().stream().filter(player -> player.hasPermissions(2))
                        .forEach(player -> player.sendSystemMessage(Component.literal(warning)));
            }
        }
        return result;
    }

    public boolean previewPoisonSequence(ServerPlayer player, int number) {
        if (number < 1 || number > poisonSequences.get().size()) return false;
        List<ZoneGeometry> circles = isMatchActive() && number == activeSequenceNumber ? poisonSequence
                : resolvePath(poisonSequences.get().get(number - 1), number);
        BattlezoneNetwork.send(player, new dev.stya.blockzone.net.battlezone.ZonePreviewS2CPacket(
                getServerLevel().dimension().location(), true, circles));
        player.sendSystemMessage(Component.literal("Sequence " + number + ": " + circles.size() + " circles (shape grid preview)"));
        for (int i = 0; i < circles.size(); i++) {
            var circle = circles.get(i);
            player.sendSystemMessage(Component.literal("Circle " + (i + 1) + ": X=" + circle.centerX() + ", Z="
                    + circle.centerZ() + ", shape=" + circle.shape().id() + ", radius=" + circle.radius()));
        }
        return true;
    }

    public void hidePoisonPreview(ServerPlayer player) {
        BattlezoneNetwork.send(player, new dev.stya.blockzone.net.battlezone.ZonePreviewS2CPacket(
                getServerLevel().dimension().location(), false, List.of()));
    }

    public PoisonPath defaultPoisonPath() {
        return PoisonSettingsMigration.defaults(getMapCenterX(), getMapCenterZ(), getInitialPoisonRadius());
    }

    public List<PoisonPath> configuredPoisonSequences() { return poisonSequences.get(); }

    public void setPoisonSequences(List<PoisonPath> paths) {
        if (paths.isEmpty()) throw new IllegalArgumentException("Keep at least one sequence");
        for (int i = 0; i < paths.size(); i++) resolvePath(paths.get(i), i + 1);
        poisonSequences.set(List.copyOf(paths));
    }

    private void initializePoisonZone() {
        if (poisonSequences.get().isEmpty()) throw new IllegalArgumentException("Configure at least one poison sequence");
        var resolved = new java.util.ArrayList<List<ZoneGeometry>>();
        for (int i = 0; i < poisonSequences.get().size(); i++) resolved.add(resolvePath(poisonSequences.get().get(i), i + 1));
        activeSequenceNumber = getServerLevel().getRandom().nextInt(resolved.size()) + 1;
        poisonSequence = resolved.get(activeSequenceNumber - 1);
        activePoisonPath = poisonSequences.get().get(activeSequenceNumber - 1);
        activePoisonPhases = activePoisonPath.circles().stream().skip(1).toList();
        activeBaseDamage = poisonDamage.get();
        var initial = poisonSequence.get(0);
        poisonCurrentRadius = (float) initial.radius();
        poisonCurrentCenterX = initial.centerX();
        poisonCurrentCenterZ = initial.centerZ();
        poisonStageInitialized = false;
    }

    private void initializePoisonStage() {
        if (poisonPhaseIndex + 1 >= poisonSequence.size()) return;
        var target = poisonSequence.get(poisonPhaseIndex + 1);
        poisonStageStartRadius = poisonCurrentRadius;
        poisonStageStartCenterX = poisonCurrentCenterX;
        poisonStageStartCenterZ = poisonCurrentCenterZ;
        poisonStageTargetRadius = (float) target.radius();
        poisonStageTargetCenterX = target.centerX();
        poisonStageTargetCenterZ = target.centerZ();
        poisonStageInitialized = true;
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
        double damage = activePoisonPath.damageAt(poisonPhaseIndex, activeBaseDamage);
        if (damage <= 0 || getServerLevel().getGameTime() % 20 != 0) {
            return;
        }
        AreaData area = getMapArea();
        ZoneGeometry zone = new ZoneGeometry(poisonCurrentCenterX,
                ZoneGeometry.centerY(area.pos1().getY(), area.pos2().getY()),
                poisonCurrentCenterZ, poisonCurrentRadius, activePoisonPath.shape());
        DamageSource damageSource = getServerLevel().damageSources().magic();
        for (ServerTeam team : getMapTeams().getNormalTeams()) {
            for (ServerPlayer player : team.getOnline()) {
                if (team.getPlayerData(player.getUUID()).map(data -> !data.isLiving()).orElse(true)) {
                    continue;
                }
                if (!hasDeploymentProtection(player) && !zone.contains(player.getX(), player.getY(), player.getZ())) {
                    player.hurt(damageSource, (float) damage);
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
        try {
            initializePoisonZone();
        } catch (IllegalArgumentException exception) {
            broadcast(Component.literal("Battlezone cannot start: invalid poisonSequences: " + exception.getMessage()));
            return false;
        }
        var route = generateDeploymentRoute();
        if (route.isEmpty()) {
            broadcast(Component.literal("Battlezone cannot start: deploymentHeight and deploymentSpeed must be valid numbers, and the initial horizontal circle must have room for a route."));
            return false;
        }
        List<dev.stya.blockzone.equipment.StartingLoadout.Prepared> loadout;
        try { loadout = dev.stya.blockzone.equipment.StartingLoadout.prepare(startingLoadout.get()); }
        catch (IllegalArgumentException exception) {
            broadcast(Component.literal("Battlezone cannot start: invalid startingLoadout: " + exception.getMessage()));
            return false;
        }
        if (!super.start()) {
            return false;
        }
        isStart = true;
        lootRoundId = java.util.UUID.randomUUID();
        phase = MatchPhase.DEPLOYMENT;
        activeLoadout = loadout;
        activeMatchHealth = matchHealth.get().floatValue();
        activePlatePoints = armorPlatePoints.get().floatValue();
        recovery.clear();
        getMapTeams().getNormalTeams().forEach(team -> team.getOnline().forEach(player -> {
            clearCombat(player);
            MatchPlayerState.initialize(player, activeMatchHealth);
            dev.stya.blockzone.equipment.StartingLoadout.apply(player, activeLoadout);
        }));
        phaseTicks = 0;
        victoryAnnounced = false;
        resetMatchClock();
        poisonPhaseIndex = 0;
        poisonPhaseTicks = 0;
        deployment.start(route.get());
        broadcast(Component.literal("Battlezone match started; poison sequence " + activeSequenceNumber + "."));
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
            clearCombat(player);
            if (!MatchRegeneration.allowed(player)) {
                MatchPlayerState.initialize(player, activeMatchHealth);
                dev.stya.blockzone.equipment.StartingLoadout.apply(player, activeLoadout);
            }
            BattlezoneNetwork.send(player, createVisualStatePacket(player));
            BattlezoneNetwork.send(player, deployment.vehicleSnapshot(phase == MatchPhase.DEPLOYMENT));
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
    public void handleDeath(com.ptcrys.fpsmatch.core.map.DeathContext context) {
        ServerPlayer dead = context.getDeadPlayer();
        if (getMapTeams().getTeamByPlayer(dead).flatMap(team -> team.getPlayerData(dead.getUUID()))
                .map(data -> !data.isLiving()).orElse(true)) return;
        super.handleDeath(context);
        ServerPlayer player = context.getDeadPlayer();
        clearCombat(player);
        clearAirbornePlayer(player);
        // FPSMatch records elimination and restores entity health; the game type owns spectator mode.
        player.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);
        player.displayClientMessage(Component.translatable("blockzone.match.eliminated"), false);
        BattlezoneNetwork.send(player, createVisualStatePacket(player));
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
        recovery.clear();
        getMapTeams().getOnlineWithSpec().forEach(this::clearCombat);
        lootRoundId = null;
        // Drops that escaped the arena bounds must also disappear when this match ends.
        for (var entity : getServerLevel().getAllEntities()) {
            if (entity instanceof dev.stya.blockzone.loot.LootDropEntity drop && drop.belongsToMap(getMapName())) {
                drop.discard();
            }
        }
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
        clearCombat(player);
        CombatHealth.remove(player);
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
                var initial = poisonSequence.get(0);
                poisonCurrentRadius = (float) initial.radius();
                poisonCurrentCenterX = initial.centerX();
                poisonCurrentCenterZ = initial.centerZ();
                poisonStageInitialized = false;
            }
            syncVisualState(true);
        }
    }

    @Override
    public void leave(ServerPlayer player) {
        super.leave(player);
        if (getMapTeams().getTeamByPlayer(player).isEmpty()) {
            deployment.remove(player);
            clearCombat(player);
            landing.finish(player);
            BattlezoneNetwork.send(player, createVisualStatePacket(player, false));
            BattlezoneNetwork.send(player, deployment.vehicleSnapshot(false));
            PlayerStateSnapshot state = playerStates.remove(player.getUUID());
            if (state != null) {
                state.restore(player);
            }
        }
    }

    @Override
    public void configFromJson(com.google.gson.JsonElement json) {
        super.configFromJson(PoisonSettingsMigration.migrate(json, getMapCenterX(), getMapCenterZ(), getInitialPoisonRadius()));
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
        var zone = poisonSequence.get(0);
        var random = getServerLevel().getRandom();
        return FlightRoute.generateAcrossCircle(zone,
                Math.min(area.pos1().getX(), area.pos2().getX()) + 0.31,
                Math.max(area.pos1().getX(), area.pos2().getX()) + 0.69,
                Math.min(area.pos1().getZ(), area.pos2().getZ()) + 0.31,
                Math.max(area.pos1().getZ(), area.pos2().getZ()) + 0.69,
                deploymentHeight.get(), deploymentSpeed.get(),
                random.nextDouble() * Math.PI * 2, random.nextDouble() * .06 - .03);
    }

    public boolean releaseDeployment(ServerPlayer player) {
        if (!deployment.protects(player)) {
            return false;
        }
        AreaData area = getMapArea();
        ZoneGeometry zone = new ZoneGeometry(poisonCurrentCenterX,
                ZoneGeometry.centerY(area.pos1().getY(), area.pos2().getY()),
                poisonCurrentCenterZ, poisonCurrentRadius, activePoisonPath.shape());
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

    public java.util.UUID getLootRoundId() { return lootRoundId; }

    public boolean saveLootCrateEdits(java.util.List<dev.stya.blockzone.util.editor.LootCrateEdit> changes,
                                      java.util.function.Consumer<Boolean> result) {
        return sceneSnapshot.saveLootCrateEdits(changes, result);
    }

    public boolean canEditLootCrates() { return !isStart && !sceneSnapshot.isBusy(); }

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
        var vehicle = deployment.vehicleSnapshot(isStart && phase == MatchPhase.DEPLOYMENT);
        for (ServerPlayer player : getMapTeams().getOnlineWithSpec()) {
            BattlezoneNetwork.send(player, createVisualStatePacket(player));
            BattlezoneNetwork.send(player, vehicle);
        }
    }

    private ZoneStateS2CPacket createVisualStatePacket(ServerPlayer player) {
        return createVisualStatePacket(player, isStart && phase != MatchPhase.RESETTING);
    }

    private ZoneStateS2CPacket createVisualStatePacket(ServerPlayer player, boolean boundaryVisible) {
        AreaData area = getMapArea();
        boolean participant = player.serverLevel() == getServerLevel() && !player.isSpectator()
                && getMapTeams().getTeamByPlayer(player)
                .filter(team -> !team.isSpectator())
                .flatMap(team -> team.getPlayerData(player.getUUID()))
                .map(data -> data.isLiving()).orElse(false);
        return new ZoneStateS2CPacket(
                getMapName(),
                getServerLevel().dimension().location(),
                boundaryVisible,
                boundaryVisible && participant && (phase == MatchPhase.DEPLOYMENT || phase == MatchPhase.MATCH),
                area.pos1(),
                area.pos2(),
                poisonCurrentCenterX,
                poisonCurrentCenterZ,
                poisonCurrentRadius,
                boundaryTexture.get(),
                activePoisonPath == null ? dev.stya.blockzone.util.battlezone.ZoneShape.CYLINDER : activePoisonPath.shape());
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

}
