package dev.stya.blockzone.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.stya.blockzone.client.compat.XaeroMinimapCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Slice;

@Pseudo
@Mixin(targets = {"xaero.common.minimap.render.MinimapFBORenderer", "xaero.common.minimap.render.MinimapSafeModeRenderer"}, remap = false)
public abstract class XaeroMinimapOpacityMixin {
    @ModifyExpressionValue(method = {"renderChunks", "renderChunksToFBO"}, require = 0,
            at = @At(value = "INVOKE", target = "Ljava/lang/Integer;intValue()I", ordinal = 0),
            slice = @Slice(from = @At(value = "FIELD", target = "Lxaero/hud/minimap/common/config/option/MinimapProfiledConfigOptions;OPACITY:Lxaero/lib/common/config/option/RangeConfigOption;")))
    private int blockzone$opacity(int original) { return XaeroMinimapCompat.active() ? 85 : original; }

    @ModifyExpressionValue(method = {"renderChunks", "renderChunksToFBO"}, require = 0,
            at = @At(value = "FIELD", target = "Lxaero/common/settings/ModSettings;minimapOpacity:D"))
    private double blockzone$legacyOpacity(double original) { return XaeroMinimapCompat.active() ? 85 : original; }
}
