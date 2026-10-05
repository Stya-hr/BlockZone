package dev.stya.blockzone.map.battlezone;

import dev.stya.blockzone.BlockZone;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** State captured before FPSMatch changes a player's inventory, mode or spawn. */
@Mod.EventBusSubscriber(modid = BlockZone.MOD_ID)
final class PlayerStateSnapshot {
    private static final Map<UUID, PlayerStateSnapshot> PENDING = new HashMap<>();
    private final ServerLevel level;
    private final Vec3 position;
    private final boolean noGravity;
    private final net.minecraft.world.entity.Pose forcedPose;
    private final float yaw;
    private final float pitch;
    private final GameType gameMode;
    private final ListTag inventory;
    private final CompoundTag food = new CompoundTag();
    private final List<MobEffectInstance> effects;
    private final float health;
    private final float absorption;
    private final int fireTicks;
    private final int airSupply;
    private final CompoundTag abilities = new CompoundTag();
    private final int selectedSlot;
    private final int experienceLevel;
    private final int totalExperience;
    private final float experienceProgress;
    private final String scoreboardTeam;

    private PlayerStateSnapshot(ServerPlayer player) {
        level = player.serverLevel();
        position = player.position();
        noGravity = player.isNoGravity();
        forcedPose = player.getForcedPose();
        yaw = player.getYRot();
        pitch = player.getXRot();
        gameMode = player.gameMode.getGameModeForPlayer();
        inventory = player.getInventory().save(new ListTag());
        selectedSlot = player.getInventory().selected;
        player.getFoodData().addAdditionalSaveData(food);
        effects = player.getActiveEffects().stream().map(MobEffectInstance::new).toList();
        health = player.getHealth();
        absorption = player.getAbsorptionAmount();
        fireTicks = player.getRemainingFireTicks();
        airSupply = player.getAirSupply();
        player.getAbilities().addSaveData(abilities);
        experienceLevel = player.experienceLevel;
        totalExperience = player.totalExperience;
        experienceProgress = player.experienceProgress;
        scoreboardTeam = player.getTeam() == null ? null : player.getTeam().getName();
    }

    static PlayerStateSnapshot capture(ServerPlayer player) {
        return new PlayerStateSnapshot(player);
    }

    static void restoreOnLogin(UUID uuid, PlayerStateSnapshot state) {
        PENDING.put(uuid, state);
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PlayerStateSnapshot state = PENDING.remove(player.getUUID());
            if (state != null) {
                state.restore(player);
            }
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        PENDING.entrySet().removeIf(entry -> entry.getValue().level.getServer() == event.getServer());
    }

    void restore(ServerPlayer player) {
        CombatHealth.remove(player);
        player.stopRiding();
        player.setNoGravity(noGravity);
        player.setForcedPose(forcedPose);
        player.setGameMode(gameMode);
        player.getAbilities().loadSaveData(abilities);
        player.onUpdateAbilities();
        player.getInventory().load(inventory);
        player.getInventory().selected = selectedSlot;
        player.getFoodData().readAdditionalSaveData(food);
        player.removeAllEffects();
        effects.forEach(effect -> player.addEffect(new MobEffectInstance(effect)));
        player.setHealth(health);
        player.setAbsorptionAmount(absorption);
        player.setAirSupply(airSupply);
        player.setExperienceLevels(experienceLevel);
        player.totalExperience = totalExperience;
        player.experienceProgress = experienceProgress;
        player.setRemainingFireTicks(fireTicks);
        player.fallDistance = 0;
        player.setDeltaMovement(Vec3.ZERO);
        player.teleportTo(level, position.x, position.y, position.z, yaw, pitch);
        if (scoreboardTeam != null) {
            var team = player.getScoreboard().getPlayerTeam(scoreboardTeam);
            if (team != null) {
                player.getScoreboard().addPlayerToTeam(player.getScoreboardName(), team);
            }
        }
        player.inventoryMenu.broadcastChanges();
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket(selectedSlot));
    }
}
