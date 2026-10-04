package dev.stya.blockzone.loot;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.loot.LootDataId;
import net.minecraft.world.level.storage.loot.LootDataType;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

public final class LootCrateBlockEntity extends BlockEntity {
    public static final ResourceLocation DEFAULT_LOOT_TABLE = ResourceLocation.fromNamespaceAndPath("blockzone", "chests/common");
    private static final Logger LOGGER = LogUtils.getLogger();
    private ResourceLocation lootTable = DEFAULT_LOOT_TABLE;
    private long lootTableSeed;
    private boolean opened;

    public LootCrateBlockEntity(BlockPos pos, BlockState state) {
        super(LootCrateRegistry.LOOT_CRATE_ENTITY.get(), pos, state);
    }

    public void configure(ResourceLocation table, long seed) {
        lootTable = table;
        lootTableSeed = seed;
        setChanged();
    }

    public void resetOpened() {
        opened = false;
        updateAppearance();
        setChanged();
    }

    public boolean open(Player player) {
        if (!(level instanceof ServerLevel server) || !(player instanceof ServerPlayer opener)
                || player.isSpectator() || opened || getBlockState().getValue(LootCrateBlock.OPEN)
                || player.distanceToSqr(Vec3.atCenterOf(worldPosition)) > 64.0) return false;

        var maps = LootCrateAccess.mapsAt(server, worldPosition);
        if (maps.size() > 1 || (!maps.isEmpty() && !LootCrateAccess.mayLoot(maps.get(0), opener))) {
            opener.displayClientMessage(Component.translatable("blockzone.loot_crate.unavailable"), true);
            return false;
        }
        var table = server.getServer().getLootData().getElement(new LootDataId<>(LootDataType.TABLE, lootTable));
        if (table == null) {
            opener.displayClientMessage(Component.translatable("blockzone.loot_crate.missing_table", lootTable), true);
            LOGGER.warn("Loot crate at {} references missing loot table {}", worldPosition, lootTable);
            return false;
        }

        List<ItemStack> rewards;
        try {
            // Chest context supports player-dependent conditions, but luck never changes match loot weights.
            var params = new LootParams.Builder(server).withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(worldPosition))
                    .withParameter(LootContextParams.THIS_ENTITY, opener).create(LootContextParamSets.CHEST);
            rewards = table.getRandomItems(params, lootTableSeed == 0 ? server.random.nextLong() : lootTableSeed);
        } catch (RuntimeException exception) {
            LOGGER.error("Could not generate loot table {} for crate at {}", lootTable, worldPosition, exception);
            opener.displayClientMessage(Component.translatable("blockzone.loot_crate.invalid_table", lootTable), true);
            return false;
        }

        var facing = getBlockState().getValue(LootCrateBlock.FACING);
        double forward = Math.atan2(facing.getStepZ(), facing.getStepX());
        List<LootDropEntity> drops = new ArrayList<>();
        for (int i = 0; i < rewards.size(); i++) {
            ItemStack stack = rewards.get(i);
            if (stack.isEmpty()) continue;
            var drop = new LootDropEntity(LootCrateRegistry.LOOT_DROP.get(), server);
            drop.setItem(stack.copy());
            drop.setPos(worldPosition.getX() + 0.5 + facing.getStepX() * 0.25,
                    worldPosition.getY() + 0.85, worldPosition.getZ() + 0.5 + facing.getStepZ() * 0.25);
            double angle = forward + (rewards.size() == 1 ? 0 : (double)i / (rewards.size() - 1) - 0.5) * Math.PI * 0.8;
            drop.setDeltaMovement(Math.cos(angle) * 0.22, 0.28 + server.random.nextDouble() * 0.08,
                    Math.sin(angle) * 0.22);
            drop.setPickUpDelay(20);
            if (!maps.isEmpty()) drop.bindToMatch(maps.get(0));
            drops.add(drop);
        }

        // Mark consumed before spawning: synchronous duplicate interactions can never reroll this crate.
        opened = true;
        updateAppearance();
        setChanged();
        drops.forEach(server::addFreshEntity);
        server.playSound(null, worldPosition, SoundEvents.CHEST_OPEN, SoundSource.BLOCKS, 1.0F, 0.85F);
        server.gameEvent(opener, GameEvent.CONTAINER_OPEN, worldPosition);
        return true;
    }

    private void updateAppearance() {
        if (level != null && !level.isClientSide && getBlockState().getBlock() instanceof LootCrateBlock) {
            level.setBlock(worldPosition, getBlockState().setValue(LootCrateBlock.OPEN, opened), Block.UPDATE_ALL);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putString("LootTable", lootTable.toString());
        tag.putLong("LootTableSeed", lootTableSeed);
        tag.putBoolean("Opened", opened);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        ResourceLocation parsed = ResourceLocation.tryParse(tag.getString("LootTable"));
        lootTable = parsed == null ? DEFAULT_LOOT_TABLE : parsed;
        lootTableSeed = tag.getLong("LootTableSeed");
        opened = tag.getBoolean("Opened");
        updateAppearance();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        updateAppearance();
    }
}
