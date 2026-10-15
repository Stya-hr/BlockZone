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
import dev.stya.blockzone.data.persistence.BattlezoneTeamConfiguration;
import dev.stya.blockzone.deployment.DeploymentController;
import dev.stya.blockzone.deployment.FlightRoute;
import dev.stya.blockzone.deployment.LandingController;
import dev.stya.blockzone.editor.loot.LootCrateEdit;
import dev.stya.blockzone.equipment.StartingLoadout;
import dev.stya.blockzone.loot.LootDropEntity;
import dev.stya.blockzone.map.battlezone.capability.BattlezoneLoadoutCapability;
import dev.stya.blockzone.map.battlezone.capability.BattlezoneCombatCapability;
import dev.stya.blockzone.map.battlezone.capability.BattlezoneInventoryCapability;
import dev.stya.blockzone.net.BlockzoneNetwork;
import dev.stya.blockzone.util.CodecSettings;
import dev.stya.blockzone.zone.PoisonPath;
import dev.stya.blockzone.zone.PoisonPathDefaults;
import dev.stya.blockzone.map.battlezone.capability.BattlezoneZoneCapability;
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

    public BattlezoneCombatCapability.Config getCombatConfig() { return combat().config(); }

    public float getCombatHealth() { return combat().health(); }
    public float getArmorPlatePoints() { return combat().platePoints(); }
    public float getMaxCombatArmor(ServerPlayer player) { return combat().maxArmor(player); }

    public BattlezoneZoneCapability zoneCapability() {
        return CapabilityMap.getMapCapability(this, BattlezoneZoneCapability.class).orElseThrow();
    }
    private PoisonZoneController poison() { return zoneCapability().controller(); }

    private final java.util.Map<java.util.UUID, PlayerStateSnapshot> originalPlayerStates = new java.util.HashMap<>();
    private final java.util.Set<java.util.UUID> runtimePlayers = new java.util.HashSet<>();
    private final java.util.Set<java.util.UUID> disconnectedPlayers = new java.util.HashSet<>();
    public void combatHurt(ServerPlayer player) { combat().hurt(player); }

    public void tickRecovery(ServerPlayer player) { combat().tickRecovery(player); }

    private void clearCombat(ServerPlayer player) { combat().clear(player); }

    private final BattlezoneTeamRoster teamRoster = new BattlezoneTeamRoster(this);
    private final BattlezoneSceneLifecycle scene;
    private MatchPhase phase = MatchPhase.WAITING;
    private int phaseTicks;
    private boolean victoryAnnounced;
    private java.util.UUID lootRoundId;
    private final BattlezoneEliminationRule eliminationRule = new BattlezoneEliminationRule();
    private final BattlezonePresentationSync presentationSync;

    public BattlezoneMap(ServerLevel serverLevel, String mapName, AreaData areaData) {
        super(serverLevel, mapName, areaData, List.of(BattlezoneLoadoutCapability.class, BattlezoneCombatCapability.class, BattlezoneZoneCapability.class,
                BattlezoneInventoryCapability.class, dev.stya.blockzone.map.battlezone.capability.BattlezoneStatisticsCapability.class));
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

        this.presentationSync = new BattlezonePresentationSync(this, deployment);
        this.scene = new BattlezoneSceneLifecycle(this);

        // Use the FPSMatch lobby timer for Battlezone's configured countdown.
        this.readyStartEnabled.set(false);
        this.readyStartTime.set(600);
        this.autoStart.set(true);
        this.autoStartTime.set(600);
        teamRoster.reconcile();
    }

    @Override
    public void tick() {
        if (!isStart) {
            teamRoster.reconcile();
        }
        scene.tick();

        if (isMatchActive()) {
            deployment.tick();
        }
        switch (phase) {
            case WAITING -> { }
            case DEPLOYMENT -> tickDeployment();
            case MATCH -> { }
            case SETTLEMENT -> tickSettlement();
            case RESETTING -> tickResetting();
        }
        presentationSync.sync(false);
        dev.stya.blockzone.squad.SquadStateSync.tick(this);
    }

    private void tickDeployment() {
        phaseTicks++;
        if (deployment.routeFinished() && phaseTicks >= Math.max(0, deploymentSeconds.get()) * 20) {
            phase = MatchPhase.MATCH;
            phaseTicks = 0;
            broadcast(Component.literal("Battlezone deployment completed."));
            presentationSync.sync(true);
        }
    }

    private void tickSettlement() {
        phaseTicks++;
        if (phaseTicks >= Math.max(0, settlementSeconds.get()) * 20) {
            reset();
        }
    }

    private void tickResetting() {
        if (!scene.isBusy()) {
            if (!scene.lastOperationSucceeded()) {
                broadcast(Component.literal("Battlezone scene restore failed; save a new snapshot before the next match."));
            }
            phase = MatchPhase.WAITING;
            phaseTicks = 0;
            presentationSync.sync(true);
        }
    }

    public boolean previewPoisonSequence(ServerPlayer player, int number) { return poison().preview(player, number); }

    public void hidePoisonPreview(ServerPlayer player) { poison().hidePreview(player); }

    public PoisonPath defaultPoisonPath() {
        return PoisonPathDefaults.create(getMapCenterX(), getMapCenterZ(), getInitialPoisonRadius());
    }

    public List<PoisonPath> configuredPoisonSequences() { return zoneCapability().read().poisonSequences(); }

    public void setPoisonSequences(List<PoisonPath> paths) {
        zoneCapability().write(zoneCapability().read().withPaths(paths));
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
        return scene.hasValidSnapshot();
    }

    private void broadcast(Component message) {
        for (ServerPlayer player : getMapTeams().getOnlineWithSpec()) {
            player.sendSystemMessage(message);
        }
    }

    @Override
    protected boolean canAutoStart() {
        return !isDebug() && phase == MatchPhase.WAITING && hasMinimumTeams() && hasValidSnapshot() && !scene.isBusy();
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
        if (scene.isBusy()) {
            broadcast(Component.literal("Battlezone cannot start: scene restore is still in progress."));
            return false;
        }
        if (!scene.hasValidSnapshot()) {
            broadcast(Component.literal("Battlezone cannot start: save a valid scene snapshot first."));
            return false;
        }
        try {
            zoneCapability().begin();
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
        try {
            loadout = loadout().prepare();
            getMapTeams().getNormalTeams().forEach(team -> team.getOnline().forEach(dev.stya.blockzone.equipment.AccessoryCompat::requireSlots));
        }
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
        CapabilityMap.getMapCapability(this, BattlezoneInventoryCapability.class).orElseThrow().begin();
        try {
            getMapTeams().getNormalTeams().forEach(team -> team.getOnline().forEach(this::initializeParticipant));
        } catch (IllegalArgumentException exception) {
            reset();
            broadcast(Component.literal("Battlezone initial inventory rejected: " + exception.getMessage()));
            return false;
        }
        statistics().begin();
        phaseTicks = 0;
        victoryAnnounced = false;
        resetMatchClock();
        deployment.start(route.get());
        broadcast(Component.literal("Battlezone match started; poison sequence " + poison().sequenceNumber() + "."));
        presentationSync.sync(true);
        return true;
    }

    @Override
    public boolean allowJoinInProgress() {
        return isDebug() || phase == MatchPhase.DEPLOYMENT || phase == MatchPhase.MATCH;
    }

    @Override
    public MapTeams.JoinTeamResult join(ServerPlayer player) {
        teamRoster.reconcile();
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
        teamRoster.reconcile();
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
                .map(map -> map.originalPlayerStates.get(player.getUUID()))
                .orElseGet(() -> PlayerStateSnapshot.capture(player));
    }

    private MapTeams.JoinTeamResult joinConfiguredTeam(String teamName, ServerPlayer player, PlayerStateSnapshot beforeJoin) {
        // FPSMatch's join reads the setting directly instead of calling allowJoinInProgress().
        boolean previous = this.allowJoinInProgress.get();
        PlayerStateSnapshot original = originalPlayerStates.remove(player.getUUID());
        this.allowJoinInProgress.set(allowJoinInProgress());
        try {
            MapTeams.JoinTeamResult result = super.join(teamName, player);
            if (original != null) {
                if (result.isSuccess() || getMapTeams().getTeamByPlayer(player).isPresent()) {
                    originalPlayerStates.put(player.getUUID(), original);
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
            originalPlayerStates.putIfAbsent(player.getUUID(), beforeJoin);
            runtimePlayers.add(player.getUUID());
        }
        if (result.isSuccess() && isStart) {
            clearCombat(player);
            if (!MatchRegeneration.allowed(player)) {
                initializeParticipant(player);
            }
            presentationSync.send(player);
            BlockzoneNetwork.sendToPlayer(player, deployment.vehicleSnapshot(phase == MatchPhase.DEPLOYMENT));
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

    @Override
    public void recordHurtData(ServerPlayer player, net.minecraft.world.damagesource.DamageSource source, float amount) {
        super.recordHurtData(player, source, amount);
        if (amount > 0 && !dev.stya.blockzone.combat.MatchRegeneration.allowed(player)) combatHurt(player);
    }

    @Override
    public void handleDeath(com.ptcrys.fpsmatch.core.map.DeathContext context) {
        ServerPlayer dead = context.getDeadPlayer();
        if (getMapTeams().getTeamByPlayer(dead).flatMap(team -> team.getPlayerData(dead.getUUID()))
                .map(data -> !data.isLiving()).orElse(true)) return;
        var reason = dev.stya.blockzone.combat.DownedDeathContext.apply(context);
        super.handleDeath(context);
        ServerPlayer player = context.getDeadPlayer();
        dev.stya.blockzone.inventory.PlayerPanelInventory.get(player).ifPresent(data -> data.dropOnElimination(this));
        clearCombat(player);
        clearAirbornePlayer(player);
        // FPSMatch records elimination and restores entity health; the game type owns spectator mode.
        player.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);
        player.displayClientMessage(Component.translatable("blockzone.match.eliminated"), false);
        presentationSync.send(player);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(
                new dev.stya.blockzone.combat.BattlezoneDeathEvent(this, context, reason));
    }

    @Override
    public void victory() {
        if (phase != MatchPhase.MATCH || victoryAnnounced) {
            return;
        }
        var resolved = eliminationRule.resolve(BattlezoneEliminationContext.capture(this).livingTeams());
        if (resolved.isEmpty()) return;
        var result = resolved.get();
        victoryAnnounced = true;
        statistics().prepareResult(result);
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
        super.victory();
        phase = MatchPhase.SETTLEMENT;
        phaseTicks = 0;
        presentationSync.sync(true);
    }

    public dev.stya.blockzone.map.battlezone.capability.BattlezoneStatisticsCapability statistics() {
        return getCapabilityMap().get(dev.stya.blockzone.map.battlezone.capability.BattlezoneStatisticsCapability.class).orElseThrow();
    }

    public Optional<BattlezoneMatchResult> getMatchResult() { return statistics().result(); }

    @Override
    public boolean victoryGoal() {
        return eliminationRule.evaluate(BattlezoneEliminationContext.capture(this)).isPresent();
    }

    @Override
    public boolean cleanupMap() {
        if (!super.cleanupMap()) {
            return false;
        }
        return hasValidSnapshot() && scene.beginRestore();
    }

    @Override
    public void reset() {
        // BaseMap.reset posts ResetEvent but does not reset capabilities in this snapshot.
        getCapabilityMap().resetAll();
        getMapTeams().getOnlineWithSpec().forEach(this::clearPlayerRuntime);
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
        presentationSync.sync(true);
        for (ServerPlayer player : List.copyOf(getMapTeams().getOnlineWithSpec())) {
            leave(player);
            // A cancelled leave event must not keep a finished match populated.
            if (getMapTeams().getTeamByPlayer(player).isPresent()) {
                getMapTeams().leaveTeam(player);
                restorePlayerState(player);
            }
        }
        for (java.util.UUID uuid : List.copyOf(getMapTeams().getJoinedPlayersWithSpec())) {
            getMapTeams().leaveTeam(uuid);
            PlayerStateSnapshot state = originalPlayerStates.remove(uuid);
            if (state != null) {
                PlayerStateSnapshot.restoreOnLogin(uuid, state);
            }
        }
        originalPlayerStates.forEach(PlayerStateSnapshot::restoreOnLogin);
        originalPlayerStates.clear();
        runtimePlayers.clear();
        disconnectedPlayers.clear();
        phase = MatchPhase.WAITING;
        phaseTicks = 0;
        victoryAnnounced = false;
        getMapTeams().getNormalTeams().forEach(ServerTeam::resetLiving);
        if (!cleanupMap()) {
            phase = MatchPhase.WAITING;
            broadcast(Component.literal("Battlezone scene restore could not start; save a valid snapshot first."));
            presentationSync.sync(true);
            return;
        }
        phase = MatchPhase.RESETTING;
        presentationSync.sync(true);
    }

    @Override
    public void handlePlayerDisconnect(ServerPlayer player) {
        disconnectedPlayers.add(player.getUUID());
        clearPlayerRuntime(player);
        super.handlePlayerDisconnect(player);
        // Active matches retain offline membership. Keep the entry snapshot until actual leave/reset.
        if (getMapTeams().getTeamByPlayer(player).isEmpty()) restorePlayerState(player);
    }

    @Override
    public void startNewRound() {
        if (isDebug() && isStart && (phase == MatchPhase.DEPLOYMENT || phase == MatchPhase.MATCH)) {
            boolean wasDeploying = phase == MatchPhase.DEPLOYMENT;
            phase = MatchPhase.MATCH;
            phaseTicks = 0;
            if (!wasDeploying) {
                poison().restart();
            }
            presentationSync.sync(true);
        }
    }

    @Override
    public void leave(ServerPlayer player) {
        if (getMapTeams().getTeamByPlayer(player).isEmpty()) return;
        super.leave(player);
        // LeaveEvent is cancelable. Only release a session after FPSMatch removed membership.
        if (getMapTeams().getTeamByPlayer(player).isEmpty()) {
            clearPlayerRuntime(player);
            restorePlayerState(player);
        }
    }

    private void clearPlayerRuntime(ServerPlayer player) {
        if (!runtimePlayers.remove(player.getUUID())) return;
        clearCombat(player);
        CombatHealth.remove(player);
        clearAirbornePlayer(player);
        dev.stya.blockzone.inventory.PlayerPanelInventory.get(player)
                .ifPresent(dev.stya.blockzone.inventory.PlayerPanelInventory::end);
        presentationSync.send(player, false);
        BlockzoneNetwork.sendToPlayer(player, deployment.vehicleSnapshot(false));
        dev.stya.blockzone.combat.CombatStateSync.clearPlayerState(player);
    }

    public void handlePlayerReconnect(ServerPlayer player) {
        if (getMapTeams().getTeamByPlayer(player).isEmpty() || !originalPlayerStates.containsKey(player.getUUID())) return;
        disconnectedPlayers.remove(player.getUUID());
        runtimePlayers.add(player.getUUID());
    }

    private void restorePlayerState(ServerPlayer player) {
        PlayerStateSnapshot state = originalPlayerStates.remove(player.getUUID());
        boolean disconnected = disconnectedPlayers.remove(player.getUUID());
        if (state == null) return;
        if (disconnected) PlayerStateSnapshot.restoreOnLogin(player.getUUID(), state);
        else state.restore(player);
    }

    private BattlezoneLoadoutCapability loadout() {
        return CapabilityMap.getMapCapability(this, BattlezoneLoadoutCapability.class).orElseThrow();
    }

    private void initializeParticipant(ServerPlayer player) {
        combat().initialize(player);
        dev.stya.blockzone.inventory.PlayerPanelInventory.get(player).ifPresent(data -> data.begin(this));
        loadout().give(player);
        dev.stya.blockzone.inventory.PlayerPanelInventory.get(player).ifPresent(dev.stya.blockzone.inventory.PlayerPanelInventory::normalize);
    }

    @Override
    public com.google.gson.JsonElement configToJson() {
        var json = super.configToJson().getAsJsonObject();
        json.add("capabilities", getCapabilityMap().getData().encode());
        json.add("teams", BattlezoneTeamConfiguration.Entry.CODEC.listOf().encodeStart(JsonOps.INSTANCE,
                BattlezoneTeamConfiguration.capture(this)).getOrThrow(false, message -> {}));
        return json;
    }

    @Override
    public void configFromJson(com.google.gson.JsonElement json) {
        var teamJson = json.getAsJsonObject().get("teams");
        var teams = teamJson == null ? List.<BattlezoneTeamConfiguration.Entry>of()
                : BattlezoneTeamConfiguration.Entry.CODEC.listOf().parse(JsonOps.INSTANCE, teamJson)
                        .getOrThrow(false, message -> {});
        BattlezoneTeamConfiguration.validate(teams);
        super.configFromJson(json);
        var capabilities = json.getAsJsonObject().get("capabilities");
        if (capabilities != null) {
            getCapabilityMap().write(CapabilityMap.Wrapper.CODEC
                    .parse(JsonOps.INSTANCE, capabilities).getOrThrow(false, message -> {}));
        }
        if (!isStart) {
            BattlezoneTeamConfiguration.restore(this, teams);
            teamRoster.reconcile();
        }
    }

    @Override
    public void setMapArea(AreaData areaData) {
        super.setMapArea(areaData);
        if (scene != null && !isStart && areaData != null) {
            scene.onAreaChanged();
        }
    }

    public boolean saveSceneSnapshot() {
        return scene.beginSave();
    }

    int teamPlayerLimit() { return teamPlayerLimit.get(); }
    int totalPlayerLimit() { return totalPlayerLimit.get(); }
    int minimumTeamsToStart() { return minimumTeamsToStart.get(); }

    private java.util.Optional<FlightRoute> generateDeploymentRoute() {
        AreaData area = getMapArea();
        var zone = poison().initial();
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
        var zone = poison().current();
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
        return scene.saveLootCrateEdits(changes, result);
    }

    public boolean canEditLootCrates() { return !isStart && !scene.isBusy(); }

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
