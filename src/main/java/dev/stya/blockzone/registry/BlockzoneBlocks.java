package dev.stya.blockzone.registry;

import dev.stya.blockzone.BlockZone;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import dev.stya.blockzone.loot.LootCrateBlock;
import net.minecraft.world.level.block.Block;

public final class BlockzoneBlocks {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, BlockZone.MOD_ID);

    public static final RegistryObject<LootCrateBlock> LOOT_CRATE = BLOCKS.register("loot_crate", LootCrateBlock::new);
    public static final RegistryObject<LootCrateBlock> TACTICAL_LOOT_CRATE = BLOCKS.register("tactical_loot_crate",
            () -> new LootCrateBlock(-8, 24, 1, 15, 6, 2, .54));
    public static final RegistryObject<LootCrateBlock> MEDICAL_LOOT_CRATE = BLOCKS.register("medical_loot_crate",
            () -> new LootCrateBlock(-1, 17, 1, 15, 9, 3, .3));
    public static final RegistryObject<LootCrateBlock> WEATHERED_LOOT_CRATE = BLOCKS.register("weathered_loot_crate",
            () -> new LootCrateBlock(-3, 19, 0, 16, 7, 2.5, .3));
    private BlockzoneBlocks() { }
    public static void register(IEventBus bus) { BLOCKS.register(bus); }
}
