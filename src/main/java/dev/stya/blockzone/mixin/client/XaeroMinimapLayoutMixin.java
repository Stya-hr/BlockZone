package dev.stya.blockzone.mixin.client;

import dev.stya.blockzone.client.compat.XaeroMinimapCompat;
import dev.stya.blockzone.client.hud.common.HudCanvas;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import xaero.hud.minimap.module.MinimapSession;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.hud.render.module.ModuleRenderContext;

@Pseudo
@Mixin(targets = "xaero.hud.minimap.module.MinimapRenderer", remap = false)
public abstract class XaeroMinimapLayoutMixin {
    @Inject(method = "render(Lxaero/hud/minimap/module/MinimapSession;Lxaero/hud/render/module/ModuleRenderContext;Lnet/minecraft/client/gui/GuiGraphics;F)V", at = @At("HEAD"), cancellable = true)
    private void blockzone$position(MinimapSession session, ModuleRenderContext context, GuiGraphics gui, float partial, CallbackInfo ci) {
        if (!XaeroMinimapCompat.active()) return;
        if (!dev.stya.blockzone.client.hud.notice.NoticeHud.minimapZoneVisible()) {
            ci.cancel();
            return;
        }
        float scale = HudCanvas.scale();
        // Xaero reserves nine source pixels on each edge for its optional frame.
        float inset = 9f * context.w / session.getConfiguredWidth();
        XaeroMinimapCompat.setWidth((context.w - 2 * inset) / scale);
        context.x = Math.round(XaeroMinimapCompat.LEFT * scale - inset);
        context.y = Math.round(context.screenHeight - XaeroMinimapCompat.BOTTOM * scale - context.h + inset);
        context.flippedHorizontally = false;
        context.flippedVertically = true;
    }
}
