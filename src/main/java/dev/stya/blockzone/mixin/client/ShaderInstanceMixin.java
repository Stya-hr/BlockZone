package dev.stya.blockzone.mixin.client;

import dev.stya.blockzone.client.battlezone.MaterialRenderState;
import net.minecraft.client.renderer.ShaderInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ShaderInstance.class)
public abstract class ShaderInstanceMixin {
    @Unique private MaterialRenderState.Bindings blockzone$bindings;
    @Inject(method = "apply", at = @At("TAIL"))
    private void blockzone$applyMaterials(CallbackInfo ci) {
        if (blockzone$bindings == null) {
            blockzone$bindings = new MaterialRenderState.Bindings(((ShaderInstance)(Object)this).getId());
        }
        blockzone$bindings.apply();
    }
}
