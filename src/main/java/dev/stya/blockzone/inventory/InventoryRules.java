package dev.stya.blockzone.inventory;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Serializable, validated map configuration. Omitted slots keep their defaults. */
public record InventoryRules(Map<String, Definition> slots) {
    public record Definition(List<String> allowed, int limit, Map<String, Integer> itemLimits, String type) {
        // Codec.optionalFieldOf treats malformed fields as absent; quantity errors must reject the update.
        private static final MapCodec<Map<String, Integer>> ITEM_LIMITS = new MapCodec<>() {
            private final Codec<Map<String, Integer>> values = Codec.unboundedMap(Codec.STRING, Codec.intRange(1, 1024));
            @Override public <T> DataResult<Map<String, Integer>> decode(DynamicOps<T> ops, MapLike<T> input) {
                T value = input.get("item_limits");
                if (value != null) return values.parse(ops, value);
                boolean present = input.entries().anyMatch(entry -> entry.getFirst().equals(ops.createString("item_limits")));
                return present ? DataResult.error(() -> "item_limits must be an object") : DataResult.success(Map.of());
            }
            @Override public <T> RecordBuilder<T> encode(Map<String, Integer> input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
                return input.isEmpty() ? prefix : prefix.add("item_limits", values.encodeStart(ops, input));
            }
            @Override public <T> java.util.stream.Stream<T> keys(DynamicOps<T> ops) {
                return java.util.stream.Stream.of(ops.createString("item_limits"));
            }
        };
        public static final Codec<Definition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.listOf().fieldOf("allowed").forGetter(Definition::allowed),
                Codec.intRange(1, 1024).fieldOf("limit").forGetter(Definition::limit),
                ITEM_LIMITS.forGetter(Definition::itemLimits),
                dev.stya.blockzone.util.CodecSettings.optionalField(Codec.STRING, "type", "").forGetter(Definition::type)).apply(instance, Definition::new));

        public Definition(List<String> allowed, int limit) { this(allowed, limit, Map.of(), ""); }
        public Definition(List<String> allowed, int limit, Map<String, Integer> itemLimits) { this(allowed, limit, itemLimits, ""); }

        public Definition {
            allowed = List.copyOf(allowed);
            itemLimits = Map.copyOf(itemLimits);
            SlotRule.parse(allowed, limit, itemLimits); // Reject malformed selectors before changing configuration.
            if (!List.of("", "accessory", "equipment", "weapon", "item", "ammo").contains(type))
                throw new IllegalArgumentException("Unknown inventory slot type: " + type);
        }

        public SlotRule compile() { return SlotRule.parse(allowed, limit, itemLimits); }
    }

    public static final Codec<InventoryRules> CODEC = Codec.unboundedMap(Codec.STRING, Codec.PASSTHROUGH)
            .flatXmap(input -> {
                var definitions = new LinkedHashMap<String, Definition>();
                try {
                    for (var entry : input.entrySet()) {
                        var decoded = dev.stya.blockzone.util.ContextualCodec.wrap(Definition.CODEC,
                                ignored -> "inventory[" + entry.getKey() + "]").parse(entry.getValue());
                        definitions.put(entry.getKey(), decoded.getOrThrow(false, message -> {}));
                    }
                    return DataResult.success(new InventoryRules(definitions));
                } catch (RuntimeException failure) {
                    return DataResult.error(failure::getMessage);
                }
            }, rules -> {
                var encoded = new LinkedHashMap<String, com.mojang.serialization.Dynamic<?>>();
                for (var entry : rules.slots().entrySet()) {
                    var json = Definition.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE, entry.getValue())
                            .getOrThrow(false, message -> {});
                    encoded.put(entry.getKey(), new com.mojang.serialization.Dynamic<>(com.mojang.serialization.JsonOps.INSTANCE, json));
                }
                return DataResult.success(encoded);
            });

    public InventoryRules {
        var merged = new LinkedHashMap<String, Definition>();
        for (var slot : InventorySlot.ALL)
            merged.put(slot.id(), new Definition(slot.defaults(), slot.defaultLimit(), Map.of(), slot.type()));
        slots.forEach((id, definition) -> {
            var slot = InventorySlot.byId(id);
            java.util.Objects.requireNonNull(definition);
            if (!definition.type().isEmpty() && !definition.type().equals(slot.type()))
                throw new IllegalArgumentException("Wrong type for " + id + ": expected " + slot.type() + ", received " + definition.type());
            if ((InventorySlot.ACCESSORIES.contains(slot) || InventorySlot.WEAPONS.contains(slot) || InventorySlot.ARMOR.contains(slot))
                    && (definition.limit() != 1 || definition.itemLimits().values().stream().anyMatch(limit -> limit != 1)))
                throw new IllegalArgumentException("Equipment, weapons and accessories have one item per slot: " + id);
            merged.put(id, new Definition(definition.allowed(), definition.limit(), definition.itemLimits(), slot.type()));
        });
        slots = java.util.Collections.unmodifiableMap(merged);
    }

    public static InventoryRules defaults() { return new InventoryRules(Map.of()); }

    /** Run after registries/tags are available; runtime admission still checks dynamic tags. */
    public void validateTypes() {
        slots.forEach((id, definition) -> {
            var slot = InventorySlot.byId(id);
            definition.allowed().forEach(selector -> ItemSelector.parse(selector).validateType(slot));
        });
    }

    public InventoryRules withSlot(InventorySlot slot, Definition definition) {
        var changed = new LinkedHashMap<>(slots);
        changed.put(slot.id(), definition);
        return new InventoryRules(changed);
    }

    public Map<InventorySlot, SlotRule> compile() {
        var result = new EnumMap<InventorySlot, SlotRule>(InventorySlot.class);
        slots.forEach((id, definition) -> result.put(InventorySlot.byId(id), definition.compile()));
        return Map.copyOf(result);
    }
}
