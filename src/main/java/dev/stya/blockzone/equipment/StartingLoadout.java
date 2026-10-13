package dev.stya.blockzone.equipment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.HashSet;
import java.util.List;

public final class StartingLoadout {
    public record Entry(String slot, ResourceLocation item, int count, String nbt) {
        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("slot").forGetter(Entry::slot),
                ResourceLocation.CODEC.fieldOf("item").forGetter(Entry::item),
                dev.stya.blockzone.util.CodecSettings.optionalField(Codec.intRange(1, 64), "count", 1).forGetter(Entry::count),
                dev.stya.blockzone.util.CodecSettings.optionalField(Codec.STRING, "nbt", "{}").forGetter(Entry::nbt)).apply(instance, Entry::new));
    }
    public record Prepared(String slot, ItemStack stack) { }
    private StartingLoadout() { }
    public static List<Entry> defaults() {
        return List.of(entry("head", "tactical_helmet", 1), entry("chest", "plate_carrier", 1),
                entry("legs", "tactical_leggings", 1), entry("feet", "tactical_boots", 1),
                entry("item.armor_plates", "armor_plate", 2));
    }
    private static Entry entry(String slot, String item, int count) {
        return new Entry(slot, ResourceLocation.fromNamespaceAndPath("blockzone", item), count, "{}");
    }
    public static List<Prepared> prepare(List<Entry> entries) {
        return prepare(entries, dev.stya.blockzone.inventory.InventoryRules.defaults().compile()::get);
    }
    public static List<Prepared> prepare(List<Entry> entries,
            java.util.function.Function<dev.stya.blockzone.inventory.InventorySlot, dev.stya.blockzone.inventory.SlotRule> rules) {
        var occupied = new HashSet<String>();
        var ammoTypes = new HashSet<dev.stya.blockzone.inventory.AmmoType>();
        return entries.stream().map(entry -> {
            // Earlier defaults staged plates in hotbar.0 before normalization; preserve that saved default.
            String configuredSlot = entry.slot.equals("hotbar.0") && entry.item.toString().equals("blockzone:armor_plate")
                    ? "item.armor_plates" : entry.slot;
            int index = inventoryIndex(configuredSlot);
            EquipmentSlot equipment = equipment(configuredSlot);
            var panel = panelSlot(configuredSlot);
            if (index < 0 && equipment == null && panel == null) throw new IllegalArgumentException("Invalid loadout slot: " + entry.slot);
            String destination = panel != null ? panel.id() : equipment == null ? "inventory:" + index : "equipment:" + equipment.getName();
            if (!occupied.add(destination)) throw new IllegalArgumentException("Duplicate loadout slot: " + entry.slot);
            var item = ForgeRegistries.ITEMS.getValue(entry.item);
            if (item == null || item == net.minecraft.world.item.Items.AIR) throw new IllegalArgumentException("Unknown loadout item: " + entry.item);
            var stack = new ItemStack(item, entry.count);
            try { stack.setTag(TagParser.parseTag(entry.nbt)); }
            catch (com.mojang.brigadier.exceptions.CommandSyntaxException exception) {
                throw new IllegalArgumentException("Invalid loadout NBT: " + entry.item, exception);
            }
            if (entry.count > stack.getMaxStackSize() || (equipment != null && entry.count != 1))
                throw new IllegalArgumentException("Invalid loadout count: " + entry.item);
            if (equipment != null && equipment != EquipmentSlot.OFFHAND
                    && (!(item instanceof net.minecraft.world.item.ArmorItem armor) || armor.getEquipmentSlot() != equipment))
                throw new IllegalArgumentException("Wrong armor slot: " + entry.item);
            if (panel != null) {
                var rule = rules.apply(panel);
                if (!panel.acceptsType(stack) || !rule.accepts(stack)
                        || stack.getCount() > rule.limitFor(stack)
                        || ((dev.stya.blockzone.inventory.InventorySlot.ACCESSORIES.contains(panel)
                        || dev.stya.blockzone.inventory.InventorySlot.WEAPONS.contains(panel)
                        || dev.stya.blockzone.inventory.InventorySlot.ARMOR.contains(panel)) && stack.getCount() != 1))
                    throw new IllegalArgumentException("Initial item violates type or capacity of " + panel.id() + ": " + entry.item);
                if (dev.stya.blockzone.inventory.InventorySlot.AMMUNITION.contains(panel)
                        && !ammoTypes.add(dev.stya.blockzone.inventory.AmmoType.of(stack)))
                    throw new IllegalArgumentException("Duplicate initial ammunition type: " + entry.item);
            }
            return new Prepared(panel == null ? entry.slot : panel.id(), stack);
        }).toList();
    }
    public static void apply(ServerPlayer player, List<Prepared> entries) {
        var inventory = dev.stya.blockzone.inventory.PlayerPanelInventory.get(player).orElseThrow();
        // Complete all contextual accessory checks before clearing or giving any item.
        for (var entry : entries) {
            var panel = panelSlot(entry.slot);
            if (panel != null && dev.stya.blockzone.inventory.InventorySlot.ACCESSORIES.contains(panel)
                    && !AccessoryCompat.canEquip(player, dev.stya.blockzone.inventory.InventorySlot.ACCESSORIES.indexOf(panel), entry.stack))
                throw new IllegalArgumentException("Initial accessory cannot equip: " + entry.slot);
        }
        player.getInventory().clearContent();
        player.getInventory().selected = 0;
        player.inventoryMenu.setCarried(ItemStack.EMPTY);
        for (int slot = 0; slot <= 4; slot++) player.inventoryMenu.getSlot(slot).set(ItemStack.EMPTY);
        for (var entry : entries) {
            var panel = panelSlot(entry.slot);
            var slot = equipment(entry.slot);
            if (panel != null) inventory.equip(panel, entry.stack);
            else if (slot != null) player.setItemSlot(slot, entry.stack.copy());
            else player.getInventory().setItem(inventoryIndex(entry.slot), entry.stack.copy());
        }
        player.inventoryMenu.broadcastChanges();
        if (player.connection != null) player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket(0));
    }
    public static dev.stya.blockzone.inventory.InventorySlot panelSlot(String id) {
        var slot = equipment(id);
        if (slot != null) return switch (slot) {
            case HEAD -> dev.stya.blockzone.inventory.InventorySlot.HELMET;
            case CHEST -> dev.stya.blockzone.inventory.InventorySlot.VEST;
            case LEGS -> dev.stya.blockzone.inventory.InventorySlot.LEGGINGS;
            case FEET -> dev.stya.blockzone.inventory.InventorySlot.BOOTS;
            case OFFHAND -> dev.stya.blockzone.inventory.InventorySlot.SHIELD;
            default -> null;
        };
        int index = inventoryIndex(id);
        if (index >= 0 && index < 2) return dev.stya.blockzone.inventory.InventorySlot.WEAPONS.get(index);
        if (index >= 2 && index < 6) return dev.stya.blockzone.inventory.InventorySlot.ITEMS.get(index - 2);
        try { return dev.stya.blockzone.inventory.InventorySlot.byId(id); }
        catch (IllegalArgumentException ignored) { return null; }
    }
    static EquipmentSlot equipment(String slot) {
        return switch (slot) { case "head" -> EquipmentSlot.HEAD; case "chest" -> EquipmentSlot.CHEST;
            case "legs" -> EquipmentSlot.LEGS; case "feet" -> EquipmentSlot.FEET; case "offhand" -> EquipmentSlot.OFFHAND; default -> null; };
    }
    static int inventoryIndex(String slot) {
        try {
            if (slot.startsWith("hotbar.")) { int value = Integer.parseInt(slot.substring(7)); return value >= 0 && value < 9 ? value : -1; }
            if (slot.startsWith("inventory.")) { int value = Integer.parseInt(slot.substring(10)); return value >= 0 && value < 27 ? value + 9 : -1; }
        } catch (NumberFormatException ignored) { }
        return -1;
    }
}
