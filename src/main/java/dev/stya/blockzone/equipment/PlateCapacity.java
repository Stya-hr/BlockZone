package dev.stya.blockzone.equipment;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;

public final class PlateCapacity {
    private PlateCapacity() { }
    public static int slots(LivingEntity player) {
        var stack = player.getItemBySlot(EquipmentSlot.CHEST);
        return stack.getItem() instanceof TacticalArmorItem armor ? armor.plateSlots()
                + (armor.plateSlots() > 0 && expanded(stack) ? 1 : 0) : 0;
    }
    public static boolean expanded(net.minecraft.world.item.ItemStack stack) {
        return stack.hasTag() && stack.getTag().getBoolean("BlockzoneArmorExpansion");
    }
    public static void setExpanded(net.minecraft.world.item.ItemStack stack, boolean value) {
        stack.getOrCreateTag().putBoolean("BlockzoneArmorExpansion", value);
    }
    public static float insert(float current, float points, int slots) {
        return Math.min(points * slots, Math.max(0, current) + points);
    }
}
