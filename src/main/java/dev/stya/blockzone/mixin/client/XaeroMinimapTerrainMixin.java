package dev.stya.blockzone.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.stya.blockzone.client.compat.XaeroMinimapCompat;
import dev.stya.blockzone.client.compat.TacticalMinimap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Slice;

@Pseudo
@Mixin(targets = "xaero.common.minimap.render.MinimapRenderer", remap = false)
public abstract class XaeroMinimapTerrainMixin {
    @ModifyVariable(method = "renderMinimap", at = @At("STORE"), name = "renderX")
    private double blockzone$centerX(double value) { return XaeroMinimapCompat.viewCenter(value, true); }

    @ModifyVariable(method = "renderMinimap", at = @At("STORE"), name = "renderZ")
    private double blockzone$centerZ(double value) { return XaeroMinimapCompat.viewCenter(value, false); }

    // Xaero draws the local player separately from the other world-positioned markers.
    @ModifyVariable(method = "renderMinimap", at = @At("STORE"), name = "centerX")
    private double blockzone$playerX(double value, @Local(name = "playerX") double player,
                                    @Local(name = "renderX") double center) {
        return XaeroMinimapCompat.active() ? value + (player - center) * XaeroMinimapCompat.zoom() : value;
    }

    @ModifyVariable(method = "renderMinimap", at = @At("STORE"), name = "centerY")
    private double blockzone$playerZ(double value, @Local(name = "playerZ") double player,
                                    @Local(name = "renderZ") double center) {
        return XaeroMinimapCompat.active() ? value + (player - center) * XaeroMinimapCompat.zoom() : value;
    }

    @ModifyExpressionValue(method = "renderMinimap", require = 0,
            at = @At(value = "INVOKE", target = "Ljava/lang/Integer;intValue()I", ordinal = 0),
            slice = @Slice(from = @At(value = "FIELD", target = "Lxaero/hud/minimap/common/config/option/MinimapProfiledConfigOptions;SHAPE:Lxaero/lib/common/config/option/RangeConfigOption;")))
    private int blockzone$square(int original) { return XaeroMinimapCompat.active() ? 0 : original; }

    @ModifyExpressionValue(method = "renderMinimap", require = 0,
            at = @At(value = "FIELD", target = "Lxaero/common/settings/ModSettings;minimapShape:I"))
    private int blockzone$legacySquare(int original) { return XaeroMinimapCompat.active() ? 0 : original; }

    @WrapOperation(method = "renderMinimap", at = @At(value = "INVOKE", ordinal = 0,
            target = "Lxaero/common/minimap/render/MinimapRendererHelper;drawMyTexturedModalRectPre(Lcom/mojang/blaze3d/vertex/PoseStack;FFIIFFFFF)V"))
    private void blockzone$terrain(@Coerce Object helper, PoseStack pose, float x, float y, int u, int v,
                                  float width, float height, float sampleHeight, float textureSize, float discardAlpha,
                                  Operation<Void> original) {
        if (!XaeroMinimapCompat.active() || !TacticalMinimap.draw(pose, x, y, u, v, width, height, sampleHeight, textureSize))
            original.call(helper, pose, x, y, u, v, width, height, sampleHeight, textureSize, discardAlpha);
    }
}
