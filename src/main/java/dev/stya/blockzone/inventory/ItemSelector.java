package dev.stya.blockzone.inventory;

import com.ptcrys.fpsmatch.compat.gun.GunCompatManager;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;

/** Thin config adapter to vanilla item predicates and FPSMatch gun providers. */
public final class ItemSelector {
    private final ResourceLocation id;
    private final boolean tag;
    private final CompoundTag requiredNbt;
    private final boolean gun;
    private volatile ItemPredicate predicate;

    private ItemSelector(ResourceLocation id, boolean tag, boolean gun, CompoundTag requiredNbt) {
        this.id = id;
        this.tag = tag;
        this.requiredNbt = requiredNbt;
        this.gun = gun;
    }

    public static ItemSelector parse(String input) {
        String value = input.trim();
        if (value.equals("*")) return new ItemSelector(null, false, false, null);
        int start = value.indexOf('{');
        String target = (start < 0 ? value : value.substring(0, start)).trim();
        boolean tag = target.startsWith("#");
        boolean gun = target.startsWith("@gun:");
        String name = tag ? target.substring(1) : gun ? target.substring(5) : target;
        ResourceLocation id = gun && name.equals("*") ? null : ResourceLocation.tryParse(name);
        if (name.isEmpty() || (id == null && !(gun && name.equals("*"))))
            throw new IllegalArgumentException("Invalid inventory item selector: " + input);
        CompoundTag nbt = null;
        if (start >= 0) {
            try { nbt = TagParser.parseTag(value.substring(start)); }
            catch (com.mojang.brigadier.exceptions.CommandSyntaxException failure) {
                throw new IllegalArgumentException("Invalid selector NBT: " + input, failure);
            }
        }
        return new ItemSelector(id, tag, gun, nbt);
    }

    public boolean matches(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (gun && (!GunCompatManager.isGun(stack)
                || (id != null && !id.equals(GunCompatManager.findProvider(stack).getGunId(stack))))) return false;
        var compiled = predicate;
        if (compiled == null) {
            var builder = ItemPredicate.Builder.item();
            if (tag) builder.of(TagKey.create(Registries.ITEM, id));
            else if (!gun && id != null) {
                var item = BuiltInRegistries.ITEM.getOptional(id);
                // Optional mods may be absent. Never resolve an unknown ID to minecraft:air.
                if (item.isEmpty()) return false;
                builder.of(item.get());
            }
            if (requiredNbt != null) builder.hasNbt(requiredNbt);
            predicate = compiled = builder.build();
        }
        return compiled.matches(stack);
    }
}
