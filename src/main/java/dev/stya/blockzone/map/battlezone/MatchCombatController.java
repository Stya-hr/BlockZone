package dev.stya.blockzone.map.battlezone;

import dev.stya.blockzone.equipment.EquipmentRegistry;
import dev.stya.blockzone.equipment.PlateCapacity;
import dev.stya.blockzone.equipment.StartingLoadout;
import net.minecraft.server.level.ServerPlayer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Frozen match loadout, health values and per-player recovery timers. */
public final class MatchCombatController {
    private float health = CombatRecovery.MAX_HEALTH;
    private float platePoints = CombatRecovery.PLATE_POINTS;
    private List<StartingLoadout.Prepared> loadout = List.of();
    private final Map<UUID, CombatRecovery> recovery = new HashMap<>();

    public void start(float health, float platePoints, List<StartingLoadout.Prepared> loadout) {
        this.health = health;
        this.platePoints = platePoints;
        this.loadout = List.copyOf(loadout);
        clearRecovery();
    }

    public void initialize(ServerPlayer player) {
        clear(player);
        MatchPlayerState.initialize(player, health);
        StartingLoadout.apply(player, loadout);
    }

    public void hurt(ServerPlayer player) {
        recovery.computeIfAbsent(player.getUUID(), id -> new CombatRecovery()).hurt();
        if (player.getUseItem().is(EquipmentRegistry.ARMOR_PLATE.get())) player.stopUsingItem();
    }

    public void tickRecovery(ServerPlayer player) {
        if (MatchRegeneration.allowed(player)) return;
        if (recovery.computeIfAbsent(player.getUUID(), id -> new CombatRecovery()).tick()
                && player.isAlive() && player.getHealth() < player.getMaxHealth()) player.heal(health * 0.05F);
    }

    public void clear(ServerPlayer player) {
        recovery.remove(player.getUUID());
        player.stopUsingItem();
        player.setAbsorptionAmount(0);
    }

    public void clearRecovery() { recovery.clear(); }
    public float health() { return health; }
    public float platePoints() { return platePoints; }
    public float maxArmor(ServerPlayer player) { return platePoints * PlateCapacity.slots(player); }
}
