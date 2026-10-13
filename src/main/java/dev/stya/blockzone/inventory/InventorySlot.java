package dev.stya.blockzone.inventory;

import java.util.Arrays;
import java.util.List;

/** Stable storage indices; new visible slots are appended so legacy inventories never shift. */
public enum InventorySlot {
    BACKPACK("equipment.backpack", "backpack", 1),
    ARMOR_EXPANSION("equipment.armor_expansion", "armor_plates", 1, "blockzone:armor_expansion"),
    HELMET("equipment.helmet", "helmet", 1, "blockzone:tactical_helmet"),
    VEST("equipment.vest", "vest", 1, "blockzone:plate_carrier"),
    SHIELD("equipment.shield", "shield", 1, "minecraft:shield"),
    UTILITY("equipment.utility", "plus", 1),
    PRIMARY_WEAPON("weapon.primary", "rifle", 1, "@gun:*"),
    SECONDARY_WEAPON("weapon.secondary", "rifle", 1, "@gun:*"),
    GRENADE("item.grenade", "grenade", 64),
    BATTERY("item.battery", "battery", 64),
    MEDKIT("item.medkit", "medkit", 64),
    ARMOR_PLATES("item.armor_plates", "plates", 64, "blockzone:armor_plate"),
    AMMO_1("ammo.1", "ammunition", 64, "tacz:ammo"),
    AMMO_2("ammo.2", "ammunition", 64, "tacz:ammo"),
    AMMO_3("ammo.3", "ammunition", 64, "tacz:ammo"),
    AMMO_4("ammo.4", "ammunition", 64, "tacz:ammo"),
    AMMO_5("ammo.5", "ammunition", 64, "tacz:ammo"),
    ACCESSORY_1("accessory.1", "accessory", 1, "*"),
    ACCESSORY_2("accessory.2", "accessory", 1, "*"),
    LEGGINGS("equipment.legs", "leggings", 1, "blockzone:tactical_leggings"),
    BOOTS("equipment.feet", "boots", 1, "blockzone:tactical_boots");

    public static final List<InventorySlot> ALL = List.of(values());
    public static final List<InventorySlot> EQUIPMENT = ALL.subList(0, 6);
    public static final List<InventorySlot> WEAPONS = ALL.subList(6, 8);
    public static final List<InventorySlot> ITEMS = ALL.subList(8, 12);
    public static final List<InventorySlot> AMMUNITION = ALL.subList(12, 17);
    public static final List<InventorySlot> ACCESSORIES = ALL.subList(17, 19);
    public static final List<InventorySlot> ARMOR = List.of(HELMET, VEST, LEGGINGS, BOOTS);
    public static final List<InventorySlot> FIRST_ROW = List.of(ACCESSORY_1, ACCESSORY_2, HELMET, VEST, LEGGINGS, BOOTS);

    /** Hard admission type; selectors can narrow a type but can never broaden it. */
    public boolean acceptsType(net.minecraft.world.item.ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (ACCESSORIES.contains(this)) return dev.stya.blockzone.equipment.AccessoryCompat.isAccessory(stack);
        if (WEAPONS.contains(this)) return com.ptcrys.fpsmatch.compat.gun.GunCompatManager.isGun(stack);
        if (ARMOR.contains(this)) return stack.getItem() instanceof net.minecraft.world.item.ArmorItem armor
                && armor.getEquipmentSlot() == equipmentSlot();
        if (this == SHIELD) return stack.canPerformAction(net.minecraftforge.common.ToolActions.SHIELD_BLOCK);
        if (this == ARMOR_EXPANSION) return stack.getItem() instanceof dev.stya.blockzone.equipment.ArmorExpansionItem;
        if ((ITEMS.contains(this) || AMMUNITION.contains(this)) && stack.getItem() instanceof net.minecraft.world.item.ArmorItem) return false;
        return !com.ptcrys.fpsmatch.compat.gun.GunCompatManager.isGun(stack)
                && !dev.stya.blockzone.equipment.AccessoryCompat.isAccessory(stack);
    }

    public String type() {
        if (ACCESSORIES.contains(this)) return "accessory";
        if (WEAPONS.contains(this)) return "weapon";
        if (AMMUNITION.contains(this)) return "ammo";
        if (EQUIPMENT.contains(this) || ARMOR.contains(this)) return "equipment";
        return "item";
    }

    public net.minecraft.world.entity.EquipmentSlot equipmentSlot() {
        return switch (this) {
            case HELMET -> net.minecraft.world.entity.EquipmentSlot.HEAD;
            case VEST -> net.minecraft.world.entity.EquipmentSlot.CHEST;
            case LEGGINGS -> net.minecraft.world.entity.EquipmentSlot.LEGS;
            case BOOTS -> net.minecraft.world.entity.EquipmentSlot.FEET;
            case SHIELD -> net.minecraft.world.entity.EquipmentSlot.OFFHAND;
            default -> null;
        };
    }

    private final String id;
    private final String icon;
    private final int defaultLimit;
    private final List<String> defaults;

    InventorySlot(String id, String icon, int defaultLimit, String... defaults) {
        this.id = id;
        this.icon = icon;
        this.defaultLimit = defaultLimit;
        this.defaults = List.of(defaults);
    }

    public String id() { return id; }
    public String icon() { return icon; }
    public int index() { return ordinal(); }
    public int defaultLimit() { return defaultLimit; }
    public List<String> defaults() { return defaults; }

    public static InventorySlot byIndex(int index) {
        if (index < 0 || index >= ALL.size()) throw new IllegalArgumentException("Invalid inventory slot index: " + index);
        return ALL.get(index);
    }

    public static InventorySlot byId(String id) {
        return Arrays.stream(values()).filter(slot -> slot.id.equals(id)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown inventory slot: " + id));
    }
}
