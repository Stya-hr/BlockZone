package dev.stya.blockzone.mixin.client;

import dev.stya.blockzone.client.compat.XaeroMinimapCompat;
import dev.stya.blockzone.client.hud.common.HudCanvas;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Stable square, north-up view with capped map-sized zoom; GUI scaling only changes its pixel footprint. */
@Pseudo
@Mixin(targets = {"xaero.common.minimap.MinimapProcessor", "xaero.hud.gui.util.GuiUtils",
        "xaero.hud.minimap.config.util.MinimapConfigClientUtils"}, remap = false)
public abstract class XaeroMinimapFixedViewMixin {
    @Inject(method = "getEffectiveMinimapSize", at = @At("HEAD"), cancellable = true, require = 0)
    private static void blockzone$configuredSize(CallbackInfoReturnable<Integer> ci) {
        if (XaeroMinimapCompat.active()) ci.setReturnValue(256);
    }
    @Inject(method = "getMinimapSize", at = @At("HEAD"), cancellable = true, require = 0)
    private void blockzone$size(CallbackInfoReturnable<Integer> ci) {
        if (XaeroMinimapCompat.active()) ci.setReturnValue(256);
    }
    @Inject(method = "getMinimapZoom", at = @At("HEAD"), cancellable = true, require = 0)
    private void blockzone$zoom(CallbackInfoReturnable<Double> ci) {
        if (XaeroMinimapCompat.active()) ci.setReturnValue(XaeroMinimapCompat.zoom());
    }
    @Inject(method = "getMinimapScale", at = @At("HEAD"), cancellable = true, require = 0)
    private static void blockzone$scale(CallbackInfoReturnable<Float> ci) {
        if (XaeroMinimapCompat.active()) ci.setReturnValue((float)(XaeroMinimapCompat.SIZE / 128
                * HudCanvas.scale() * Minecraft.getInstance().getWindow().getGuiScale()));
    }
    @Inject(method = "getEffectiveNorthLocked", at = @At("HEAD"), cancellable = true, require = 0)
    private static void blockzone$north(CallbackInfoReturnable<Boolean> ci) {
        if (XaeroMinimapCompat.active()) ci.setReturnValue(true);
    }
}
