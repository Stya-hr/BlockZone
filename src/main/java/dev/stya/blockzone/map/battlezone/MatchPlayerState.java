package dev.stya.blockzone.map.battlezone;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

/** One baseline for initial participants and players entering an active match. */
final class MatchPlayerState {
    private MatchPlayerState() { }

    static void initialize(ServerPlayer player, float maxHealth) {
        player.closeContainer();
        player.stopUsingItem();
        player.stopRiding();
        player.removeAllEffects();
        player.setGameMode(GameType.ADVENTURE);
        var abilities = player.getAbilities();
        abilities.invulnerable = false;
        abilities.instabuild = false;
        abilities.mayfly = false;
        abilities.flying = false;
        abilities.mayBuild = false;
        abilities.setWalkingSpeed(0.1F);
        abilities.setFlyingSpeed(0.05F);
        player.onUpdateAbilities();
        player.setNoGravity(false);
        player.setForcedPose(null);
        player.setSprinting(false);
        player.setShiftKeyDown(false);
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0;
        player.clearFire();
        player.setAirSupply(player.getMaxAirSupply());
        player.setTicksFrozen(0);
        player.hurtTime = 0;
        player.deathTime = 0;
        player.invulnerableTime = 0;
        player.setLastHurtByMob(null);
        player.setLastHurtByPlayer(null);
        player.getFoodData().readAdditionalSaveData(fullFood());
        player.setExperienceLevels(0);
        player.totalExperience = 0;
        player.experienceProgress = 0;
        player.getInventory().clearContent();
        player.getInventory().selected = 0;
        player.inventoryMenu.setCarried(ItemStack.EMPTY);
        for (int slot = 0; slot <= 4; slot++) player.inventoryMenu.getSlot(slot).set(ItemStack.EMPTY);
        ForgeRegistries.ITEMS.getValues().stream().filter(player.getCooldowns()::isOnCooldown)
                .forEach(player.getCooldowns()::removeCooldown);
        player.resetAttackStrengthTicker();
        CombatHealth.initialize(player, maxHealth);
        player.setAbsorptionAmount(0);
        player.inventoryMenu.broadcastChanges();
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket(0));
    }

    static CompoundTag fullFood() {
        var food = new CompoundTag();
        food.putInt("foodLevel", 20);
        food.putInt("foodTickTimer", 0);
        food.putFloat("foodSaturationLevel", 5);
        food.putFloat("foodExhaustionLevel", 0);
        return food;
    }
}
