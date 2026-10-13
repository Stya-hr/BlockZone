package dev.stya.blockzone.inventory;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;

/** Physical ammunition identity, independent of count and cosmetic NBT. */
public record AmmoType(boolean tacz, ResourceLocation id) {
    public static AmmoType of(ItemStack stack) {
        if (stack.isEmpty()) throw new IllegalArgumentException("Empty stacks have no ammunition type");
        if (ModList.get().isLoaded("tacz")) {
            ResourceLocation id = Tacz.id(stack);
            if (id != null) return new AmmoType(true, id);
        }
        // Unidict may use any registered item as ammunition, not just tacz:ammo.
        return new AmmoType(false, BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }

    // Keep optional TaCZ classes isolated so storage works without that mod installed.
    private static final class Tacz {
        static ResourceLocation id(ItemStack stack) {
            if (stack.getItem() instanceof com.tacz.guns.api.item.IAmmo ammo) return ammo.getAmmoId(stack);
            if (stack.getItem() instanceof com.tacz.guns.api.item.IAmmoBox box) return box.getAmmoId(stack);
            return null;
        }
    }
}
