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
        var target = InventorySlot.byIndex(slot);
        if (!rules.apply(target).accepts(stack)) return false;
        if (InventorySlot.AMMUNITION.contains(target)) {
            var type = AmmoType.of(stack);
            for (var other : InventorySlot.AMMUNITION) {
                var existing = getStackInSlot(other.index());
                if (other != target && !existing.isEmpty() && type.equals(AmmoType.of(existing))) return false;
            }
        }
        return true;
    }

    /** One type occupies one slot; even a full or NBT-incompatible stack never spills into a second slot. */
    public ItemStack insertAmmo(ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        var type = AmmoType.of(stack);
        for (var slot : InventorySlot.AMMUNITION) {
            var existing = getStackInSlot(slot.index());
            if (!existing.isEmpty() && type.equals(AmmoType.of(existing)))
                return insertItem(slot.index(), stack, simulate);
        }
        for (var slot : InventorySlot.AMMUNITION)
            if (getStackInSlot(slot.index()).isEmpty() && isItemValid(slot.index(), stack))
                return insertItem(slot.index(), stack, simulate);
        return stack;
    }

    @Override
    public int getSlotLimit(int slot) {
        return rules.apply(InventorySlot.byIndex(slot)).maximumLimit();
    }

    @Override
    protected int getStackLimit(int slot, ItemStack stack) {
        return Math.min(rules.apply(InventorySlot.byIndex(slot)).limitFor(stack), stack.getMaxStackSize());
    }

    @Override
    public void setStackInSlot(int slot, ItemStack stack) {
        InventorySlot.byIndex(slot);
        if (!stack.isEmpty() && (!isItemValid(slot, stack)
                || stack.getCount() > getStackLimit(slot, stack)))
            throw new IllegalArgumentException("Item or count is not allowed in " + InventorySlot.byIndex(slot).id());
        super.setStackInSlot(slot, stack.copy());
    }

    @Override
    public void setSize(int size) {
        if (size != InventorySlot.ALL.size()) throw new IllegalArgumentException("Panel inventory must have 17 slots");
        super.setSize(size);
    }

    @Override
    public void deserializeNBT(net.minecraft.nbt.CompoundTag nbt) {
        // Validate before mutating this inventory; never discard or merge legacy duplicate stacks.
        if (nbt.getInt("Size") != InventorySlot.ALL.size())
            throw new IllegalArgumentException("Panel inventory must have 17 slots");
        var loaded = new ItemStackHandler(InventorySlot.ALL.size());
        loaded.deserializeNBT(nbt);
        var types = new java.util.HashSet<AmmoType>();
        for (var slot : InventorySlot.AMMUNITION) {
            var stack = loaded.getStackInSlot(slot.index());
            if (!stack.isEmpty() && !types.add(AmmoType.of(stack)))
                throw new IllegalArgumentException("Duplicate ammunition type in saved inventory");
        }
        super.deserializeNBT(nbt);
    }

    // Inherited NBT loading preserves existing items even when new rules no longer admit them.
    // They can still be extracted; new insertion and direct replacement always validate.
}
