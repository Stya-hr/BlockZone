package dev.stya.blockzone.equipment;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;

public final class PlateCapacity {
    private PlateCapacity() { }
    public static int slots(LivingEntity player) {
        var stack = player.getItemBySlot(EquipmentSlot.CHEST);
        return stack.getItem() instanceof TacticalArmorItem armor ? armor.plateSlots()
                + (armor.plateSlots() > 0 && expanded(player) ? 1 : 0) : 0;
    }
    public static boolean expanded(LivingEntity entity) {
        if (!(entity instanceof net.minecraft.world.entity.player.Player player)) return false;
        var external = AccessoryCompat.inventory(player);
        if (external.isPresent()) {
            for (int i = 0; i < external.get().getSlots(); i++)
                if (external.get().getStackInSlot(i).getItem() instanceof ArmorExpansionItem) return true;
            return false;
        }
        return dev.stya.blockzone.inventory.PlayerPanelInventory.get(player)
                .filter(dev.stya.blockzone.inventory.PlayerPanelInventory::active)
                .map(inventory -> dev.stya.blockzone.inventory.InventorySlot.ACCESSORIES.stream()
                        .anyMatch(slot -> inventory.getStack(slot).getItem() instanceof ArmorExpansionItem))
                .orElse(false);
    }
    public static float insert(float current, float points, int slots) {
        return Math.min(points * slots, Math.max(0, current) + points);
    }
}
