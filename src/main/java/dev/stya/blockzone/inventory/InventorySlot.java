package dev.stya.blockzone.inventory;

import java.util.Arrays;
import java.util.List;

/** Stable IDs and storage indices for the 17 slots in the inventory panel. */
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
    AMMO_5("ammo.5", "ammunition", 64, "tacz:ammo");

    public static final List<InventorySlot> ALL = List.of(values());
    public static final List<InventorySlot> EQUIPMENT = ALL.subList(0, 6);
    public static final List<InventorySlot> WEAPONS = ALL.subList(6, 8);
    public static final List<InventorySlot> ITEMS = ALL.subList(8, 12);
    public static final List<InventorySlot> AMMUNITION = ALL.subList(12, 17);

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
