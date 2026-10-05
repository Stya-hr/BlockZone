package dev.stya.blockzone.equipment;

import dev.stya.blockzone.BlockZone;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Equipment registration is independent of loot containers and their contents. */
public final class EquipmentRegistry {
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

    private EquipmentRegistry() { }

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
        bus.addListener(EquipmentRegistry::creativeTab);
    }

    private static void creativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() != CreativeModeTabs.COMBAT) return;
        event.accept(ARMOR_PLATE);
        event.accept(TACTICAL_HELMET);
        event.accept(PLATE_CARRIER);
        event.accept(ARMOR_EXPANSION);
        event.accept(TACTICAL_LEGGINGS);
        event.accept(TACTICAL_BOOTS);
    }
}
