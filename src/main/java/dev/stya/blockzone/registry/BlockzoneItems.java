package dev.stya.blockzone.registry;

import dev.stya.blockzone.equipment.ArmorPlateItem;
import dev.stya.blockzone.equipment.ArmorExpansionItem;
import dev.stya.blockzone.equipment.TacticalArmorItem;

import dev.stya.blockzone.BlockZone;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** All item registrations and the BlockZone creative tab. */
public final class BlockzoneItems {
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, BlockZone.MOD_ID);

    public static final RegistryObject<Item> ARMOR_PLATE = ITEMS.register("armor_plate", ArmorPlateItem::new);
    public static final RegistryObject<Item> TACTICAL_HELMET = ITEMS.register("tactical_helmet",
            () -> new TacticalArmorItem(ArmorItem.Type.HELMET, "helmet", 0));
    public static final RegistryObject<Item> PLATE_CARRIER = ITEMS.register("plate_carrier",
            () -> new TacticalArmorItem(ArmorItem.Type.CHESTPLATE, "carrier", 2));
    public static final RegistryObject<Item> ARMOR_EXPANSION = ITEMS.register("armor_expansion", ArmorExpansionItem::new);
    public static final RegistryObject<Item> TACTICAL_LEGGINGS = ITEMS.register("tactical_leggings",
            () -> new TacticalArmorItem(ArmorItem.Type.LEGGINGS, "leggings", 0));
    public static final RegistryObject<Item> TACTICAL_BOOTS = ITEMS.register("tactical_boots",
            () -> new TacticalArmorItem(ArmorItem.Type.BOOTS, "boots", 0));

    public static final RegistryObject<Item> LOOT_CRATE_ITEM = ITEMS.register("loot_crate",
            () -> new BlockItem(BlockzoneBlocks.LOOT_CRATE.get(), new Item.Properties()));
    public static final RegistryObject<Item> TACTICAL_LOOT_CRATE_ITEM = ITEMS.register("tactical_loot_crate",
            () -> new BlockItem(BlockzoneBlocks.TACTICAL_LOOT_CRATE.get(), new Item.Properties()));
    public static final RegistryObject<Item> MEDICAL_LOOT_CRATE_ITEM = ITEMS.register("medical_loot_crate",
            () -> new BlockItem(BlockzoneBlocks.MEDICAL_LOOT_CRATE.get(), new Item.Properties()));
    public static final RegistryObject<Item> WEATHERED_LOOT_CRATE_ITEM = ITEMS.register("weathered_loot_crate",
            () -> new BlockItem(BlockzoneBlocks.WEATHERED_LOOT_CRATE.get(), new Item.Properties()));

    private static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, BlockZone.MOD_ID);
    public static final RegistryObject<CreativeModeTab> BLOCKZONE = TABS.register("blockzone",
            () -> CreativeModeTab.builder().title(Component.translatable("itemGroup.blockzone"))
                    .icon(() -> ARMOR_PLATE.get().getDefaultInstance())
                    .displayItems((parameters, output) -> ITEMS.getEntries().forEach(item -> output.accept(item.get())))
                    .build());

    private BlockzoneItems() { }

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
        TABS.register(bus);
    }
}
