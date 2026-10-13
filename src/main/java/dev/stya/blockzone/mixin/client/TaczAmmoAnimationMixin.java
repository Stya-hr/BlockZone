package dev.stya.blockzone.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.stya.blockzone.inventory.PlayerPanelInventory;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(targets = "com.tacz.guns.client.animation.statemachine.GunAnimationStateContext", remap = false)
public abstract class TaczAmmoAnimationMixin {
    @WrapOperation(method = "*", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getCapability(Lnet/minecraftforge/common/capabilities/Capability;Lnet/minecraft/core/Direction;)Lnet/minecraftforge/common/util/LazyOptional;", remap = false), require = 0)
    private <T> LazyOptional<T> blockzone$ammunition(Entity entity, Capability<T> type, Direction side, Operation<LazyOptional<T>> original) {
        if (entity instanceof Player player && type == ForgeCapabilities.ITEM_HANDLER && side == null) {
            var data = PlayerPanelInventory.get(player).filter(PlayerPanelInventory::active).orElse(null);
            if (data != null) return data.ammunition().cast();
        }
        return original.call(entity, type, side);
    }
}
