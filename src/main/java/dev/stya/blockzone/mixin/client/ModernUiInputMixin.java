package dev.stya.blockzone.mixin.client;

import dev.stya.blockzone.client.ui.ModernUiScreen;
import icyllis.modernui.mc.UIManager;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Native mod screens forward scaled pointer input once, including editor world viewports. */
@Mixin(value = UIManager.class, remap = false)
public abstract class ModernUiInputMixin {
    @Inject(method = "onPostMouseInput", at = @At("HEAD"), cancellable = true)
    private void blockzone$screenMouse(int button, int action, int modifiers, CallbackInfo callback) {
        if (Minecraft.getInstance().screen instanceof ModernUiScreen) callback.cancel();
    }
    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void blockzone$screenScroll(double x, double y, CallbackInfo callback) {
        if (Minecraft.getInstance().screen instanceof ModernUiScreen) callback.cancel();
    }
    @Inject(method = "onHoverMove", at = @At("HEAD"), cancellable = true)
    private void blockzone$screenHover(boolean natural, CallbackInfo callback) {
        if (Minecraft.getInstance().screen instanceof ModernUiScreen) callback.cancel();
    }
}
