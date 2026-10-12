package dev.stya.blockzone.inventory;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Serializable, validated map configuration. Omitted slots keep their defaults. */
public record InventoryRules(Map<String, Definition> slots) {
    public record Definition(List<String> allowed, int limit) {
        public static final Codec<Definition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.listOf().fieldOf("allowed").forGetter(Definition::allowed),
                Codec.intRange(1, 1024).fieldOf("limit").forGetter(Definition::limit)).apply(instance, Definition::new));

        public Definition {
            allowed = List.copyOf(allowed);
            SlotRule.parse(allowed, limit); // Reject malformed selectors before changing configuration.
        }

        public SlotRule compile() { return SlotRule.parse(allowed, limit); }
    }

    public static final Codec<InventoryRules> CODEC = Codec.unboundedMap(Codec.STRING, Definition.CODEC)
            .xmap(InventoryRules::new, InventoryRules::slots);

    public InventoryRules {
        var merged = new LinkedHashMap<String, Definition>();
        for (var slot : InventorySlot.ALL)
            merged.put(slot.id(), new Definition(slot.defaults(), slot.defaultLimit()));
        slots.forEach((id, definition) -> {
            InventorySlot.byId(id); // Reject typos rather than silently ignoring a slot.
            merged.put(id, java.util.Objects.requireNonNull(definition));
        });
        slots = java.util.Collections.unmodifiableMap(merged);
    }

    public static InventoryRules defaults() { return new InventoryRules(Map.of()); }

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
