package dev.stya.blockzone.loot;

import com.ptcrys.fpsmatch.core.FPSMCore;
import dev.stya.blockzone.map.battlezone.BattlezoneMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
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
    }

    public void bindToMatch(BattlezoneMap map) {
        mapName = map.getMapName();
        roundId = map.getLootRoundId();
    }

    public boolean belongsToMap(String name) { return mapName.equals(name); }

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
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
