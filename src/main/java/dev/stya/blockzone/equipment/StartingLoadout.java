package dev.stya.blockzone.equipment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
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
        return List.of(entry("equipment.helmet", "tactical_helmet", 1), entry("equipment.vest", "plate_carrier", 1),
                entry("equipment.legs", "tactical_leggings", 1), entry("equipment.feet", "tactical_boots", 1),
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
            var panel = dev.stya.blockzone.inventory.InventorySlot.byId(entry.slot);
            String destination = panel.id();
            if (!occupied.add(destination)) throw new IllegalArgumentException("Duplicate loadout slot: " + entry.slot);
            var item = ForgeRegistries.ITEMS.getValue(entry.item);
            if (item == null || item == net.minecraft.world.item.Items.AIR) throw new IllegalArgumentException("Unknown loadout item: " + entry.item);
            var stack = new ItemStack(item, entry.count);
            try { stack.setTag(TagParser.parseTag(entry.nbt)); }
            catch (com.mojang.brigadier.exceptions.CommandSyntaxException exception) {
                throw new IllegalArgumentException("Invalid loadout NBT: " + entry.item, exception);
            }
            if (entry.count > stack.getMaxStackSize())
                throw new IllegalArgumentException("Invalid loadout count: " + entry.item);
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

            return new Prepared(panel.id(), stack);
        }).toList();
    }
    public static void apply(ServerPlayer player, List<Prepared> entries) {
        var inventory = dev.stya.blockzone.inventory.PlayerPanelInventory.get(player).orElseThrow();
        // Complete all contextual accessory checks before clearing or giving any item.
        for (var entry : entries) {
            var panel = panelSlot(entry.slot);
            if (dev.stya.blockzone.inventory.InventorySlot.ACCESSORIES.contains(panel)
                    && !AccessoryCompat.canEquip(player, dev.stya.blockzone.inventory.InventorySlot.ACCESSORIES.indexOf(panel), entry.stack))
                throw new IllegalArgumentException("Initial accessory cannot equip: " + entry.slot);
        }
        player.getInventory().clearContent();
        player.getInventory().selected = 0;
        player.inventoryMenu.setCarried(ItemStack.EMPTY);
        for (int slot = 0; slot <= 4; slot++) player.inventoryMenu.getSlot(slot).set(ItemStack.EMPTY);
        for (var entry : entries) {
            var panel = panelSlot(entry.slot);
            inventory.equip(panel, entry.stack);
        }
        player.inventoryMenu.broadcastChanges();
        if (player.connection != null) player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket(0));
    }
    public static dev.stya.blockzone.inventory.InventorySlot panelSlot(String id) {
        return dev.stya.blockzone.inventory.InventorySlot.byId(id);
    }
}
