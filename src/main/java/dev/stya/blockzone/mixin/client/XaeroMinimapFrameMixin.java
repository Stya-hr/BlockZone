package dev.stya.blockzone.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.stya.blockzone.client.compat.XaeroMinimapCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Slice;

@Pseudo
@Mixin(targets = "xaero.common.minimap.render.MinimapRenderer", remap = false)
public abstract class XaeroMinimapFrameMixin {
    @ModifyExpressionValue(method = "renderMinimap", require = 0, at = @At(value = "INVOKE", target = "Ljava/lang/Integer;intValue()I"),
            slice = @Slice(from = @At(value = "FIELD", target = "Lxaero/hud/minimap/common/config/option/MinimapProfiledConfigOptions;FRAME:Lxaero/lib/common/config/option/RangeConfigOption;"),
                    to = @At(value = "FIELD", target = "Lxaero/hud/minimap/common/config/MinimapConfigConstants;FRAME_NAMES:[Lnet/minecraft/network/chat/Component;")))
    private int blockzone$borderless(int original) {
        return XaeroMinimapCompat.active() ? 3 : original; // Xaero's native "off" frame.
    }
    @ModifyExpressionValue(method = "renderMinimap", require = 0,
            at = @At(value = "FIELD", target = "Lxaero/common/settings/ModSettings;minimapFrame:I"))
    private int blockzone$legacyBorderless(int original) {
        return XaeroMinimapCompat.active() ? 3 : original;
    }
}
