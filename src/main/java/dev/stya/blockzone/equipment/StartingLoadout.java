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
                entry("hotbar.0", "armor_plate", 2));
    }
    private static Entry entry(String slot, String item, int count) {
        return new Entry(slot, ResourceLocation.fromNamespaceAndPath("blockzone", item), count, "{}");
    }
    public static List<Prepared> prepare(List<Entry> entries) {
        var occupied = new HashSet<String>();
        return entries.stream().map(entry -> {
            int index = inventoryIndex(entry.slot);
            EquipmentSlot equipment = equipment(entry.slot);
            if (index < 0 && equipment == null) throw new IllegalArgumentException("Invalid loadout slot: " + entry.slot);
            String destination = equipment == null ? "inventory:" + index : "equipment:" + equipment.getName();
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
            return new Prepared(entry.slot, stack);
        }).toList();
    }
    public static void apply(ServerPlayer player, List<Prepared> entries) {
        for (var entry : entries) {
            var slot = equipment(entry.slot);
            if (slot != null) player.setItemSlot(slot, entry.stack.copy());
            else player.getInventory().setItem(inventoryIndex(entry.slot), entry.stack.copy());
        }
        player.inventoryMenu.broadcastChanges();
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
