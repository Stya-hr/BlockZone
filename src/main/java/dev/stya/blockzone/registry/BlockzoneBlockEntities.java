package dev.stya.blockzone.registry;

import dev.stya.blockzone.BlockZone;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import dev.stya.blockzone.loot.LootCrateBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class BlockzoneBlockEntities {
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, BlockZone.MOD_ID);

    public static final RegistryObject<BlockEntityType<LootCrateBlockEntity>> LOOT_CRATE_ENTITY =
            BLOCK_ENTITIES.register("loot_crate", () -> BlockEntityType.Builder
                    .of(LootCrateBlockEntity::new, BlockzoneBlocks.LOOT_CRATE.get(), BlockzoneBlocks.TACTICAL_LOOT_CRATE.get(),
                            BlockzoneBlocks.MEDICAL_LOOT_CRATE.get(), BlockzoneBlocks.WEATHERED_LOOT_CRATE.get()).build(null));
    private BlockzoneBlockEntities() { }
    public static void register(IEventBus bus) { BLOCK_ENTITIES.register(bus); }
}
