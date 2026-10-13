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
        private static final Codec<Entry> FIELDS = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("slot").forGetter(Entry::slot),
                ResourceLocation.CODEC.fieldOf("item").forGetter(Entry::item),
                dev.stya.blockzone.util.CodecSettings.optionalField(Codec.intRange(1, 64), "count", 1).forGetter(Entry::count),
                dev.stya.blockzone.util.CodecSettings.optionalField(Codec.STRING, "nbt", "{}").forGetter(Entry::nbt)).apply(instance, Entry::new));
        public static final Codec<Entry> CODEC = dev.stya.blockzone.util.ContextualCodec.wrap(FIELDS,
                input -> "loadout[slot=" + input.get("slot").asString("<missing>")
                        + ", item=" + input.get("item").asString("<missing>") + "]");
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
        var ammoTypes = new java.util.HashMap<dev.stya.blockzone.inventory.AmmoType, String>();
        return entries.stream().map(entry -> {
            String context = "loadout[slot=" + entry.slot + ", item=" + entry.item + "]: ";
            dev.stya.blockzone.inventory.InventorySlot panel;
            try { panel = dev.stya.blockzone.inventory.InventorySlot.byCategory(entry.slot).stream()
                    .filter(candidate -> !occupied.contains(candidate.id())).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("no free slot of category " + entry.slot)); }
            catch (IllegalArgumentException failure) { throw new IllegalArgumentException(context + failure.getMessage()); }
            String destination = panel.id();
            if (!occupied.add(destination)) throw new IllegalArgumentException(context + "destination is configured more than once");
            var item = ForgeRegistries.ITEMS.getValue(entry.item);
            if (item == null || item == net.minecraft.world.item.Items.AIR) throw new IllegalArgumentException(context + "item is not registered");
            var stack = new ItemStack(item, entry.count);
            try { stack.setTag(TagParser.parseTag(entry.nbt)); }
            catch (com.mojang.brigadier.exceptions.CommandSyntaxException exception) {
                throw new IllegalArgumentException(context + "invalid NBT: " + exception.getMessage(), exception);
            }
            if (entry.count < 1 || entry.count > stack.getMaxStackSize())
                throw new IllegalArgumentException(context + "count " + entry.count + " exceeds native range 1.." + stack.getMaxStackSize());
            var rule = rules.apply(panel);
            if (!panel.acceptsType(stack))
                throw new IllegalArgumentException(context + "expected " + panel.type()
                        + (panel.equipmentSlot() == null ? "" : " for " + panel.equipmentSlot().getName()));
            if (!rule.accepts(stack)) throw new IllegalArgumentException(context + "item/NBT does not match allowed selectors");
            int maximum = Math.min(stack.getMaxStackSize(), rule.limitFor(stack));
            if (dev.stya.blockzone.inventory.InventorySlot.FIRST_ROW.contains(panel)
                    || dev.stya.blockzone.inventory.InventorySlot.WEAPONS.contains(panel)) maximum = Math.min(1, maximum);
            if (stack.getCount() > maximum)
                throw new IllegalArgumentException(context + "count " + stack.getCount() + " exceeds slot limit " + maximum);
            if (dev.stya.blockzone.inventory.InventorySlot.AMMUNITION.contains(panel)) {
                var previous = ammoTypes.putIfAbsent(dev.stya.blockzone.inventory.AmmoType.of(stack), panel.id());
                if (previous != null) throw new IllegalArgumentException(context + "ammunition type already occupies " + previous);
            }

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
