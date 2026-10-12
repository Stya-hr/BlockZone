package dev.stya.blockzone.inventory;

import java.util.List;
import java.util.Map;
import net.minecraft.world.item.ItemStack;

/** Selectors are OR-ed; an empty list disables insertion. */
public record SlotRule(List<ItemSelector> selectors, int limit, List<ItemLimit> itemLimits) {
    public record ItemLimit(ItemSelector selector, int limit) {}

    public SlotRule {
        selectors = List.copyOf(selectors);
        itemLimits = List.copyOf(itemLimits);
        if (limit < 1 || limit > 1024) throw new IllegalArgumentException("Slot limit must be between 1 and 1024");
        for (var entry : itemLimits)
            if (entry.limit() < 1 || entry.limit() > 1024)
                throw new IllegalArgumentException("Item limit must be between 1 and 1024");
    }

    public static SlotRule parse(List<? extends String> selectors, int limit) {
        return parse(selectors, limit, Map.of());
    }

    public static SlotRule parse(List<? extends String> selectors, int limit, Map<String, Integer> itemLimits) {
        for (String selector : itemLimits.keySet())
            if (!selectors.contains(selector)) throw new IllegalArgumentException("Item limit selector must appear in allowed: " + selector);
        return new SlotRule(selectors.stream().map(ItemSelector::parse).toList(), limit,
                itemLimits.entrySet().stream().map(entry -> new ItemLimit(ItemSelector.parse(entry.getKey()), entry.getValue())).toList());
    }

    /** Matching overrides replace the fallback; overlapping overrides use the strictest limit. */
    public int limitFor(ItemStack stack) {
        if (!accepts(stack)) return 0;
        return itemLimits.stream().filter(entry -> entry.selector().matches(stack))
                .mapToInt(ItemLimit::limit).min().orElse(limit);
    }

    public int maximumLimit() {
        return Math.max(limit, itemLimits.stream().mapToInt(ItemLimit::limit).max().orElse(limit));
    }

    public boolean accepts(ItemStack stack) {
        return !stack.isEmpty() && selectors.stream().anyMatch(selector -> selector.matches(stack));
    }
}
