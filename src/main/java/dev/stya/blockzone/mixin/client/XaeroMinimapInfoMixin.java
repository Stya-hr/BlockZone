package dev.stya.blockzone.mixin.client;

import dev.stya.blockzone.client.compat.XaeroMinimapCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Reserve the space above the map for BlockZone's zone status. */
@Pseudo
@Mixin(targets = {"xaero.hud.minimap.info.render.InfoDisplayRenderer",
        "xaero.hud.minimap.compass.render.CompassRenderer", "xaero.hud.minimap.waypoint.render.WaypointMapRenderer"}, remap = false)
public abstract class XaeroMinimapInfoMixin {
    @Inject(method = {"render", "drawCompass"}, require = 0, at = @At("HEAD"), cancellable = true)
    private void blockzone$hideInfo(CallbackInfo ci) {
        if (XaeroMinimapCompat.active()) ci.cancel();
    }
    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true, require = 0)
    private void blockzone$hideWaypoints(CallbackInfoReturnable<Boolean> ci) {
        if (XaeroMinimapCompat.active()) ci.setReturnValue(false);
    }
}
