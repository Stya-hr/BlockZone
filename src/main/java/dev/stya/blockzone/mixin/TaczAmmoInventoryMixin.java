package dev.stya.blockzone.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.stya.blockzone.inventory.PlayerPanelInventory;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

/** Replace only TaCZ ammunition-source queries; leave the global Forge item handler unchanged. */
@Pseudo
@Mixin(targets = {"com.tacz.guns.api.item.gun.AbstractGunItem", "com.tacz.guns.item.ModernKineticGunScriptAPI", "com.tacz.guns.entity.shooter.LivingEntityShoot"}, remap = false)
public abstract class TaczAmmoInventoryMixin {
    @WrapOperation(method = "*", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getCapability(Lnet/minecraftforge/common/capabilities/Capability;Lnet/minecraft/core/Direction;)Lnet/minecraftforge/common/util/LazyOptional;", remap = false), require = 0)
    private <T> LazyOptional<T> blockzone$ammunition(LivingEntity entity, Capability<T> type, Direction side, Operation<LazyOptional<T>> original) {
        if (entity instanceof Player player && type == ForgeCapabilities.ITEM_HANDLER && side == null) {
            var data = PlayerPanelInventory.get(player).filter(PlayerPanelInventory::active).orElse(null);
            if (data != null) return data.ammunition().cast();
        }
        return original.call(entity, type, side);
    }
}
