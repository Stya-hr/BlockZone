package dev.stya.blockzone.loot;

import dev.stya.blockzone.BlockZone;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class LootCrateRegistry {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, BlockZone.MOD_ID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, BlockZone.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, BlockZone.MOD_ID);
    private static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, BlockZone.MOD_ID);

    public static final RegistryObject<LootCrateBlock> LOOT_CRATE = BLOCKS.register("loot_crate", LootCrateBlock::new);
    public static final RegistryObject<Item> LOOT_CRATE_ITEM = ITEMS.register("loot_crate",
            () -> new BlockItem(LOOT_CRATE.get(), new Item.Properties()));
    public static final RegistryObject<BlockEntityType<LootCrateBlockEntity>> LOOT_CRATE_ENTITY =
            BLOCK_ENTITIES.register("loot_crate", () -> BlockEntityType.Builder
                    .of(LootCrateBlockEntity::new, LOOT_CRATE.get()).build(null));
    public static final RegistryObject<EntityType<LootDropEntity>> LOOT_DROP = ENTITIES.register("loot_drop",
            () -> EntityType.Builder.<LootDropEntity>of(LootDropEntity::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F).clientTrackingRange(8).updateInterval(2)
                    .build(BlockZone.MOD_ID + ":loot_drop"));

    private LootCrateRegistry() { }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
        ENTITIES.register(bus);
        bus.addListener(LootCrateRegistry::creativeTab);
    }

    private static void creativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) event.accept(LOOT_CRATE_ITEM);
    }
}
