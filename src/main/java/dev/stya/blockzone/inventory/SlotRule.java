package dev.stya.blockzone.inventory;

import java.util.List;
import net.minecraft.world.item.ItemStack;

/** Selectors are OR-ed; an empty list disables insertion. */
public record SlotRule(List<ItemSelector> selectors, int limit) {
    public SlotRule {
        selectors = List.copyOf(selectors);
        if (limit < 1 || limit > 1024) throw new IllegalArgumentException("Slot limit must be between 1 and 1024");
    }

    public static SlotRule parse(List<? extends String> selectors, int limit) {
        return new SlotRule(selectors.stream().map(ItemSelector::parse).toList(), limit);
    }

    public boolean accepts(ItemStack stack) {
        return !stack.isEmpty() && selectors.stream().anyMatch(selector -> selector.matches(stack));
    }
}
