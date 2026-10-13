package dev.stya.blockzone.mixin;

import dev.stya.blockzone.inventory.PlayerPanelInventory;
import dev.stya.blockzone.combat.DownedController;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Keep vanilla ownership, delay, Forge pickup events, partial quantities and statistics. */
@Mixin(ItemEntity.class)
public abstract class InventoryPickupMixin {
    @Redirect(method = "playerTouch", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Inventory;add(Lnet/minecraft/world/item/ItemStack;)Z"))
    private boolean blockzone$pickUp(Inventory inventory, ItemStack stack) {
        var data = PlayerPanelInventory.get(inventory.player).filter(PlayerPanelInventory::active).orElse(null);
        if (data == null || !data.manages(stack)) return inventory.add(stack);
        if (!inventory.player.isAlive() || inventory.player.isSpectator()
                || inventory.player instanceof ServerPlayer server && DownedController.busy(server)) return false;
        int before = stack.getCount();
        var remainder = (Object) this instanceof dev.stya.blockzone.loot.LootDropEntity
                ? data.pickUpReplacingEquipment(stack) : data.insert(stack, false);
        stack.setCount(remainder.getCount());
        return stack.getCount() < before;
    }
}
