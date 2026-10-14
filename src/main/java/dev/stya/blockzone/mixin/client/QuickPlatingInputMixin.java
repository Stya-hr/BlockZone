package dev.stya.blockzone.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.stya.blockzone.client.equipment.QuickPlating;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Minecraft.class)
public abstract class QuickPlatingInputMixin {
    @WrapOperation(method = "handleKeybinds", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/KeyMapping;isDown()Z"))
    private boolean blockzone$holdPlateUse(KeyMapping mapping, Operation<Boolean> original) {
        return QuickPlating.holdsUse(mapping) || original.call(mapping);
    }
}
