package dev.stya.blockzone.inventory;

import java.util.Objects;
import java.util.function.Function;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;

/** Server-side storage foundation for the panel; every insertion uses the latest slot rules. */
public final class PanelInventory extends ItemStackHandler {
    private final Function<InventorySlot, SlotRule> rules;

    public PanelInventory(Function<InventorySlot, SlotRule> rules) {
        super(InventorySlot.ALL.size());
        this.rules = Objects.requireNonNull(rules);
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return rules.apply(InventorySlot.byIndex(slot)).accepts(stack);
    }

    @Override
    public int getSlotLimit(int slot) {
        return rules.apply(InventorySlot.byIndex(slot)).limit();
    }

    @Override
    public void setStackInSlot(int slot, ItemStack stack) {
        InventorySlot.byIndex(slot);
        if (!stack.isEmpty() && (!isItemValid(slot, stack)
                || stack.getCount() > Math.min(getSlotLimit(slot), stack.getMaxStackSize())))
            throw new IllegalArgumentException("Item or count is not allowed in " + InventorySlot.byIndex(slot).id());
        super.setStackInSlot(slot, stack.copy());
    }

    @Override
    public void setSize(int size) {
        if (size != InventorySlot.ALL.size()) throw new IllegalArgumentException("Panel inventory must have 17 slots");
        super.setSize(size);
    }

    // Inherited NBT loading preserves existing items even when new rules no longer admit them.
    // They can still be extracted; new insertion and direct replacement always validate.
}
