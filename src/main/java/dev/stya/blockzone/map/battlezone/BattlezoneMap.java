package dev.stya.blockzone.map.battlezone;

import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.ptcrys.fpsmatch.core.capability.CapabilityMap;
import com.ptcrys.fpsmatch.core.data.AreaData;
import com.ptcrys.fpsmatch.core.data.Setting;
import com.ptcrys.fpsmatch.core.map.BaseMap;
import com.ptcrys.fpsmatch.core.team.MapTeams;
import com.ptcrys.fpsmatch.core.team.ServerTeam;
import com.ptcrys.fpsmatch.core.team.TeamData;
import dev.stya.blockzone.combat.CombatHealth;
import dev.stya.blockzone.combat.MatchCombatController;
import dev.stya.blockzone.combat.MatchRegeneration;
import dev.stya.blockzone.data.snapshot.PlayerStateSnapshot;
import dev.stya.blockzone.data.snapshot.SceneSnapshot;
import dev.stya.blockzone.deployment.DeploymentController;
import dev.stya.blockzone.deployment.FlightRoute;
import dev.stya.blockzone.deployment.LandingController;
import dev.stya.blockzone.editor.loot.LootCrateEdit;
import dev.stya.blockzone.equipment.StartingLoadout;
import dev.stya.blockzone.loot.LootDropEntity;
import dev.stya.blockzone.map.battlezone.capability.BattlezoneLoadoutCapability;
import dev.stya.blockzone.map.battlezone.capability.BattlezoneCombatCapability;
import dev.stya.blockzone.net.battlezone.BattlezoneNetwork;
import dev.stya.blockzone.util.CodecSettings;
import dev.stya.blockzone.zone.PoisonPath;
import dev.stya.blockzone.zone.PoisonSettingsMigration;
import dev.stya.blockzone.zone.PoisonZoneController;
import dev.stya.blockzone.zone.ZoneGeometry;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BattlezoneMap extends BaseMap {
    public static final String GAME_TYPE = "battlezone";
    private static final Logger LOGGER = LoggerFactory.getLogger(BattlezoneMap.class);

    private final Setting<Integer> teamPlayerLimit;
    private final Setting<Integer> totalPlayerLimit;
    private final Setting<Integer> minimumTeamsToStart;
    private final Setting<Integer> deploymentSeconds;
    private final Setting<Integer> settlementSeconds;
    private final Setting<Double> deploymentSpeed;
    private final Setting<Double> deploymentHeight;
    private final DeploymentController deployment = new DeploymentController(this);
    private final LandingController landing = new LandingController();
    private MatchCombatController combat() { return CapabilityMap.getMapCapability(this, BattlezoneCombatCapability.class).orElseThrow().controller(); }

    public float getCombatHealth() { return combat().health(); }
    public float getArmorPlatePoints() { return combat().platePoints(); }
    public float getMaxCombatArmor(ServerPlayer player) { return combat().maxArmor(player); }

    private final Setting<Double> poisonDamage;
    private final Setting<List<PoisonPath>> poisonSequences;
    private final PoisonZoneController poison = new PoisonZoneController(this);
    private final Setting<String> boundaryTexture;

    private final java.util.Map<java.util.UUID, PlayerStateSnapshot> playerStates = new java.util.HashMap<>();
    public void combatHurt(ServerPlayer player) { combat().hurt(player); }

    public void tickRecovery(ServerPlayer player) { combat().tickRecovery(player); }

    private void clearCombat(ServerPlayer player) { combat().clear(player); }

    private final SceneSnapshot sceneSnapshot;
    private MatchPhase phase = MatchPhase.WAITING;
    private int phaseTicks;
    private boolean snapshotValid;
    private boolean snapshotSavePending;
    private boolean victoryAnnounced;
    private java.util.UUID lootRoundId;
    private final BattlezoneEliminationRule eliminationRule = new BattlezoneEliminationRule();
    private final BattlezoneVisualSync visualSync;

    public BattlezoneMap(ServerLevel serverLevel, String mapName, AreaData areaData) {
        super(serverLevel, mapName, areaData, List.of(BattlezoneLoadoutCapability.class, BattlezoneCombatCapability.class));
        this.teamPlayerLimit = addSetting("battlezone", "teamPlayerLimit", 3);
        this.totalPlayerLimit = addSetting("battlezone", "totalPlayerLimit", 24);
        this.minimumTeamsToStart = addSetting("battlezone", "minimumTeamsToStart", 2);
        this.deploymentSeconds = addSetting("battlezone", "deploymentSeconds", 15);
        this.settlementSeconds = addSetting("battlezone", "settlementSeconds", 10);
        this.deploymentSpeed = addSetting(CodecSettings.create("battlezone", "deploymentSpeed",
                Codec.doubleRange(0.1, 100.0), 20.0));
        this.deploymentHeight = addSetting(CodecSettings.create("battlezone", "deploymentHeight",
                CodecSettings.FINITE_DOUBLE,
                Math.max(areaData.pos1().getY(), areaData.pos2().getY()) + 64.0));

        this.poisonDamage = addSetting(CodecSettings.create("battlezone", "poisonDamagePerSecond",
                CodecSettings.NONNEGATIVE_DOUBLE, 1.0));
        this.poisonSequences = addSetting(CodecSettings.create("battlezone", "poisonSequences", PoisonPath.CODEC.listOf(),
                List.of(PoisonSettingsMigration.defaults(getMapCenterX(), getMapCenterZ(), getInitialPoisonRadius()))));
        this.boundaryTexture = addSetting("battlezone", "boundaryTexture",
                "blockzone:textures/effect/battlezone_warning_fence.png");
        this.visualSync = new BattlezoneVisualSync(this, deployment, poison, boundaryTexture::get);
        this.sceneSnapshot = new SceneSnapshot(this);
        this.snapshotValid = sceneSnapshot.load();
        if (!snapshotValid && !sceneSnapshot.exists()) {
            // New maps capture their initial baseline over several map ticks.
            sceneSnapshot.beginSave();
        }

        // Use the FPSMatch lobby timer for Battlezone's configured countdown.
        this.readyStartEnabled.set(false);
        this.autoStart.set(true);
        this.autoStartTime.set(600);
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
            case WAITING -> { }
            case DEPLOYMENT -> tickDeployment();
            case MATCH -> tickMatch();
            case SETTLEMENT -> tickSettlement();
            case RESETTING -> tickResetting();
        }
        visualSync.sync(false);
    }

    private void tickDeployment() {
        phaseTicks++;
        if (deployment.routeFinished() && phaseTicks >= Math.max(0, deploymentSeconds.get()) * 20) {
            phase = MatchPhase.MATCH;
            phaseTicks = 0;
            broadcast(Component.literal("Battlezone deployment completed."));
            visualSync.sync(true);
        }
    }

    private void tickMatch() {
        poison.tick();
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
            visualSync.sync(true);
        }
    }

    public boolean previewPoisonSequence(ServerPlayer player, int number) { return poison.preview(player, number); }

    public void hidePoisonPreview(ServerPlayer player) { poison.hidePreview(player); }

    public PoisonPath defaultPoisonPath() {
        return PoisonSettingsMigration.defaults(getMapCenterX(), getMapCenterZ(), getInitialPoisonRadius());
    }

    public List<PoisonPath> configuredPoisonSequences() { return poisonSequences.get(); }

    public void setPoisonSequences(List<PoisonPath> paths) {
        if (paths.isEmpty()) throw new IllegalArgumentException("Keep at least one sequence");
        for (int i = 0; i < paths.size(); i++) poison.resolvePath(paths.get(i), i + 1);
        poisonSequences.set(List.copyOf(paths));
    }

    private double getMapCenterX() {
        AreaData area = getMapArea();
        return (area.pos1().getX() + area.pos2().getX() + 1.0) / 2.0;
    }

    private double getMapCenterZ() {
        AreaData area = getMapArea();
        return (area.pos1().getZ() + area.pos2().getZ() + 1.0) / 2.0;
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
        return !isDebug() && phase == MatchPhase.WAITING && hasMinimumTeams() && hasValidSnapshot() && !sceneSnapshot.isBusy();
    }

    @Override
    protected boolean canReadyStart() {
        return canAutoStart() && super.canReadyStart();
    }

    @Override
    public boolean start() {
        if (isStart || (!isDebug() && !hasMinimumTeams())) {
            return false;
        }
        if (phase != MatchPhase.WAITING) {
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
            poison.initialize(poisonSequences.get(), poisonDamage.get());
        } catch (IllegalArgumentException exception) {
            broadcast(Component.literal("Battlezone cannot start: invalid poisonSequences: " + exception.getMessage()));
            return false;
        }
        var route = generateDeploymentRoute();
        if (route.isEmpty()) {
            broadcast(Component.literal("Battlezone cannot start: deploymentHeight and deploymentSpeed must be valid numbers, and the initial horizontal circle must have room for a route."));
            return false;
        }
        List<StartingLoadout.Prepared> loadout;
        try { loadout = loadout().prepare(); }
        catch (IllegalArgumentException exception) {
            broadcast(Component.literal("Battlezone cannot start: invalid loadout capability: " + exception.getMessage()));
            return false;
        }
        if (!super.start()) {
            return false;
        }
        isStart = true;
        lootRoundId = java.util.UUID.randomUUID();
        phase = MatchPhase.DEPLOYMENT;
        loadout().begin(loadout);
        CapabilityMap.getMapCapability(this, BattlezoneCombatCapability.class).orElseThrow().begin();
        getMapTeams().getNormalTeams().forEach(team -> team.getOnline().forEach(this::initializeParticipant));
        phaseTicks = 0;
        victoryAnnounced = false;
        resetMatchClock();
        deployment.start(route.get());
        broadcast(Component.literal("Battlezone match started; poison sequence " + poison.sequenceNumber() + "."));
        visualSync.sync(true);
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
                initializeParticipant(player);
            }
            visualSync.send(player);
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
        visualSync.send(player);
    }

    @Override
    public void victory() {
        if (phase != MatchPhase.MATCH || victoryAnnounced) {
            return;
        }
        victoryAnnounced = true;
        eliminationRule.resolve(BattlezoneMatchContext.capture(this).livingTeams()).ifPresent(result -> {
            if (result.reason() == BattlezoneResultReason.DRAW) {
                broadcast(Component.literal("Battlezone ended in a draw."));
            } else {
                getMapTeams().getTeamByName(result.winner()).ifPresent(winner -> {
                    winner.sendMessage(Component.literal("Your team won the Battlezone."));
                    for (ServerTeam team : getMapTeams().getNormalTeams()) {
                        if (team != winner) team.sendMessage(Component.literal("Your team was eliminated from the Battlezone."));
                    }
                });
            }
        });
        super.victory();
        phase = MatchPhase.SETTLEMENT;
        phaseTicks = 0;
        visualSync.sync(true);
    }

    @Override
    public boolean victoryGoal() {
        return eliminationRule.evaluate(BattlezoneMatchContext.capture(this)).isPresent();
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
        loadout().reset();
        combat().clearRecovery();
        getMapTeams().getOnlineWithSpec().forEach(this::clearCombat);
        lootRoundId = null;
        // Drops that escaped the arena bounds must also disappear when this match ends.
        for (var entity : getServerLevel().getAllEntities()) {
            if (entity instanceof LootDropEntity drop && drop.belongsToMap(getMapName())) {
                drop.discard();
            }
        }
        deployment.clear();
        landing.clear();
        super.reset();
        isStart = false;
        visualSync.sync(true);
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
        poison.reset(new ZoneGeometry(getMapCenterX(), 0, getMapCenterZ(), getInitialPoisonRadius()));
        victoryAnnounced = false;
        getMapTeams().getNormalTeams().forEach(ServerTeam::resetLiving);
        if (!cleanupMap()) {
            phase = MatchPhase.WAITING;
            broadcast(Component.literal("Battlezone scene restore could not start; save a valid snapshot first."));
            visualSync.sync(true);
            return;
        }
        phase = MatchPhase.RESETTING;
        visualSync.sync(true);
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
                poison.restart();
            }
            visualSync.sync(true);
        }
    }

    @Override
    public void leave(ServerPlayer player) {
        super.leave(player);
        if (getMapTeams().getTeamByPlayer(player).isEmpty()) {
            deployment.remove(player);
            clearCombat(player);
            landing.finish(player);
            visualSync.send(player, false);
            BattlezoneNetwork.send(player, deployment.vehicleSnapshot(false));
            PlayerStateSnapshot state = playerStates.remove(player.getUUID());
            if (state != null) {
                state.restore(player);
            }
        }
    }

    private BattlezoneLoadoutCapability loadout() {
        return CapabilityMap.getMapCapability(this, BattlezoneLoadoutCapability.class).orElseThrow();
    }

    private void initializeParticipant(ServerPlayer player) {
        combat().initialize(player);
        loadout().give(player);
    }

    @Override
    public com.google.gson.JsonElement configToJson() {
        var json = super.configToJson().getAsJsonObject();
        json.add("capabilities", getCapabilityMap().getData().encode());
        return json;
    }

    @Override
    public void configFromJson(com.google.gson.JsonElement json) {
        super.configFromJson(PoisonSettingsMigration.migrate(json, getMapCenterX(), getMapCenterZ(), getInitialPoisonRadius()));
        var capabilities = json.getAsJsonObject().get("capabilities");
        if (capabilities != null) {
            getCapabilityMap().write(CapabilityMap.Wrapper.CODEC
                    .parse(JsonOps.INSTANCE, capabilities).getOrThrow(false, message -> {}));
        }
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
        var zone = poison.initial();
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
        var zone = poison.current();
        if (player.serverLevel() != getServerLevel()
                || !zone.containsHorizontal(player.getX(), player.getZ())) {
            player.displayClientMessage(Component.translatable("blockzone.deployment.outside_zone"), true);
            return false;
        }
        return deployment.release(player);
    }

    public void clearAirbornePlayer(ServerPlayer player) {
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

    public void tickDeploymentPlayer(ServerPlayer player) {
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

    public boolean saveLootCrateEdits(java.util.List<LootCrateEdit> changes,
                                      java.util.function.Consumer<Boolean> result) {
        return sceneSnapshot.saveLootCrateEdits(changes, result);
    }

    public boolean canEditLootCrates() { return !isStart && !sceneSnapshot.isBusy(); }

    public boolean isMatchActive() {
        return isStart && phase != MatchPhase.RESETTING;
    }

    @Override
    public String getGameType() {
        return GAME_TYPE;
    }

    public enum MatchPhase {
        WAITING,
        DEPLOYMENT,
        MATCH,
        SETTLEMENT,
        RESETTING
    }

}
