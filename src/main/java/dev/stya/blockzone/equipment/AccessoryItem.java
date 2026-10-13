package dev.stya.blockzone.equipment;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Base for native accessories; supports standalone equipment and delegates to Curios when installed. */
public abstract class AccessoryItem extends Item {
    protected AccessoryItem(Properties properties) { super(properties.stacksTo(1)); }
    public boolean canEquip(Player player, int slot, ItemStack stack) { return true; }
    public boolean canUnequip(Player player, int slot, ItemStack stack) { return true; }
    public void onEquip(Player player, int slot, ItemStack stack) {}
    public void onUnequip(Player player, int slot, ItemStack stack) {}
    public void accessoryTick(Player player, int slot, ItemStack stack) {}
}
