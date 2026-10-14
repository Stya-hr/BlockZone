package dev.stya.blockzone.combat;

import dev.stya.blockzone.map.battlezone.capability.BattlezoneCombatCapability;
import dev.stya.blockzone.registry.BlockzoneItems;
import dev.stya.blockzone.equipment.PlateCapacity;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;

/** Frozen match health values and per-player recovery timers. */
public final class MatchCombatController {
    private BattlezoneCombatCapability.Config config =
            BattlezoneCombatCapability.defaults();
    private float health = CombatRecovery.MAX_HEALTH;
    private float platePoints = CombatRecovery.PLATE_POINTS;
    private final Map<UUID, CombatRecovery> recovery = new HashMap<>();

    public void start(BattlezoneCombatCapability.Config config) {
        this.config = config;
        this.health = (float) config.matchHealth();
        this.platePoints = (float) config.armorPlatePoints();
        clearRecovery();
    }

    public void initialize(ServerPlayer player) {
        clear(player);
        MatchPlayerState.initialize(player, health);
    }

    public void hurt(ServerPlayer player) {
        recovery.computeIfAbsent(player.getUUID(), id -> newRecovery()).hurt();
        if (player.getUseItem().is(BlockzoneItems.ARMOR_PLATE.get())) player.stopUsingItem();
    }

    public void tickRecovery(ServerPlayer player) {
        if (MatchRegeneration.allowed(player) || DownedController.down(player)) return;
        if (recovery.computeIfAbsent(player.getUUID(), id -> newRecovery()).tick()
                && player.isAlive() && player.getHealth() < player.getMaxHealth()) player.heal((float) (health * config.recoveryHealthFraction()));
    }

    public BattlezoneCombatCapability.Config config() { return config; }
    private CombatRecovery newRecovery() {
        return new CombatRecovery(
                BattlezoneCombatCapability.Config.ticks(config.recoveryDelaySeconds()),
                BattlezoneCombatCapability.Config.ticks(config.recoveryIntervalSeconds()));
    }

    public void clear(ServerPlayer player) {
        DownedController.clear(player);
        recovery.remove(player.getUUID());
        player.stopUsingItem();
        player.setAbsorptionAmount(0);
    }

    public void clearRecovery() { recovery.clear(); }
    public float health() { return health; }
    public float platePoints() { return platePoints; }
    public float maxArmor(ServerPlayer player) { return platePoints * PlateCapacity.slots(player); }
}
