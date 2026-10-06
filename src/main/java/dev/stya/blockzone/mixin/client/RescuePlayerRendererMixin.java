package dev.stya.blockzone.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.stya.blockzone.client.battlezone.RescueClientState;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerRenderer.class)
public abstract class RescuePlayerRendererMixin {
    @Inject(method = "setupRotations", at = @At("HEAD"), cancellable = true)
    private void blockzone$rescueBody(AbstractClientPlayer player, PoseStack stack,
                                     float age, float yaw, float partialTick, CallbackInfo ci) {
        int state = RescueClientState.state(player);
        if (state == 0) return;
        stack.mulPose(Axis.YP.rotationDegrees(180 - yaw));
        if (state == 1) {
            stack.mulPose(Axis.XP.rotationDegrees(90));
            stack.translate(0, -1, -.3);
        } else {
            stack.translate(0, -.25, 0);
        }
        ci.cancel();
    }
}
