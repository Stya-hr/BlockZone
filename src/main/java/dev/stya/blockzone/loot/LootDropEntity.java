package dev.stya.blockzone.loot;

import com.ptcrys.fpsmatch.core.FPSMCore;
import dev.stya.blockzone.map.battlezone.BattlezoneMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkHooks;

import java.util.UUID;

/** Reuses item physics, inventory insertion and full stack NBT, with a dedicated stationary ground renderer. */
public final class LootDropEntity extends ItemEntity {
    private String mapName = "";
    private UUID roundId;

    public LootDropEntity(EntityType<? extends LootDropEntity> type, Level level) {
        super(type, level);
        // Vanilla also excludes unlimited-lifetime items from merging, preserving the scattered display.
        setUnlimitedLifetime();
        setGlowingTag(true);
    }

    public void bindToMatch(BattlezoneMap map) {
        mapName = map.getMapName();
        roundId = map.getLootRoundId();
    }

    public boolean belongsToMap(String name) { return mapName.equals(name); }

    @Override public boolean isPickable() { return isAlive(); }

    @Override public boolean skipAttackInteraction(net.minecraft.world.entity.Entity attacker) { return true; }

    // Keep vanilla insertion, ownership, pickup delay, statistics and partial-stack handling,
    // but only invoke them from an explicit interaction, never from walking over the item.
    @Override public void playerTouch(Player player) { }

    @Override public InteractionResult interact(Player player, InteractionHand hand) {
        if (player.isSpectator() || !player.isAlive() || hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (!level().isClientSide && player.distanceToSqr(this) <= 9 && player.hasLineOfSight(this)) {
            super.playerTouch(player);
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    private BattlezoneMap owningMap() {
        return FPSMCore.getInstance().getMapByTypeWithName(BattlezoneMap.GAME_TYPE, mapName)
                .filter(BattlezoneMap.class::isInstance).map(BattlezoneMap.class::cast)
                .filter(map -> map.getServerLevel() == level()).orElse(null);
    }

    @Override
    public void tick() {
        if (!level().isClientSide && !mapName.isEmpty()) {
            var map = owningMap();
            if (map == null || roundId == null || !roundId.equals(map.getLootRoundId())) {
                discard();
                return;
            }
        }
        super.tick();
    }

    @Override
    public float getSpin(float partialTicks) { return getYRot() * (float)Math.PI / 180.0F; }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("BattlezoneMap", mapName);
        if (roundId != null) tag.putUUID("BattlezoneRound", roundId);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        mapName = tag.getString("BattlezoneMap");
        roundId = tag.hasUUID("BattlezoneRound") ? tag.getUUID("BattlezoneRound") : null;
        setUnlimitedLifetime();
        setGlowingTag(true);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
