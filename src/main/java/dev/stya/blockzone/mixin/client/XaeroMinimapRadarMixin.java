package dev.stya.blockzone.mixin.client;

import com.llamalad7.mixinextras.sugar.Local;
import dev.stya.blockzone.client.compat.XaeroMinimapCompat;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.hud.minimap.player.tracker.PlayerTrackerMinimapElement;

/** Whitelist the current squad instead of exposing Xaero's entity/enemy radar. */
@Pseudo
@Mixin(targets = {"xaero.hud.minimap.radar.render.element.RadarElementReader",
        "xaero.hud.minimap.player.tracker.PlayerTrackerMinimapElementReader",
        "xaero.common.minimap.render.MinimapSafeModeRenderer"}, remap = false)
public abstract class XaeroMinimapRadarMixin {
    @Inject(method = "isHidden(Lnet/minecraft/world/entity/Entity;Lxaero/hud/minimap/radar/render/element/RadarRenderContext;)Z",
            at = @At("HEAD"), cancellable = true, require = 0)
    private void blockzone$filterRadar(CallbackInfoReturnable<Boolean> ci, @Local(argsOnly = true) Entity entity) {
        if (XaeroMinimapCompat.active()) ci.setReturnValue((!(entity instanceof net.minecraft.world.entity.player.Player) || !XaeroMinimapCompat.visiblePlayer(entity.getUUID()) || !XaeroMinimapCompat.withinMap(entity.getX(), entity.getZ())));
    }
    @Inject(method = "isHidden(Lxaero/hud/minimap/player/tracker/PlayerTrackerMinimapElement;Lxaero/hud/minimap/player/tracker/PlayerTrackerMinimapElementRenderContext;)Z",
            at = @At("HEAD"), cancellable = true, require = 0)
    private void blockzone$filterTracked(CallbackInfoReturnable<Boolean> ci, @Local(argsOnly = true) PlayerTrackerMinimapElement<?> player) {
        if (XaeroMinimapCompat.active() && (!XaeroMinimapCompat.visiblePlayer(player.getPlayerId()) || !XaeroMinimapCompat.withinMap(player.getX(), player.getZ()))) ci.setReturnValue(true);
    }
    @Inject(method = "renderEntityDotSafeMode", at = @At("HEAD"), cancellable = true, require = 0)
    private void blockzone$filterSafeRadar(CallbackInfoReturnable<Boolean> ci, @Local(argsOnly = true, ordinal = 1) Entity entity) {
        if (XaeroMinimapCompat.active() && (!(entity instanceof net.minecraft.world.entity.player.Player) || !XaeroMinimapCompat.visiblePlayer(entity.getUUID()) || !XaeroMinimapCompat.withinMap(entity.getX(), entity.getZ()))) ci.setReturnValue(false);
    }
}
