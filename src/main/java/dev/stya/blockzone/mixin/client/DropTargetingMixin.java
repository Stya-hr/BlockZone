package dev.stya.blockzone.mixin.client;

import dev.stya.blockzone.client.loot.DropTargeting;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class DropTargetingMixin {
    @Inject(method = "pick", at = @At("RETURN"))
    private void blockzone$pickDrop(float partial, CallbackInfo ci) { DropTargeting.pick(partial); }
}
