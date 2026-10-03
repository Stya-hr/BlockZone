package dev.stya.blockzone.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.stya.blockzone.client.battlezone.FlightVisualPose;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerRenderer.class)
public abstract class FlightPlayerRendererMixin {
    @Redirect(method = "setupRotations", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/player/AbstractClientPlayer;getSwimAmount(F)F"))
    private float blockzone$useFlightRotation(AbstractClientPlayer player, float partialTick) {
        return FlightVisualPose.get(player) == null ? player.getSwimAmount(partialTick) : 0;
    }

    @Inject(method = "setupRotations", at = @At("TAIL"))
    private void blockzone$rotateFlightBody(AbstractClientPlayer player, PoseStack stack,
            float age, float yaw, float partialTick, CallbackInfo callback) {
        var pose = FlightVisualPose.get(player);
        if (pose != null) {
            stack.mulPose(Axis.XP.rotationDegrees(pose.value(0, partialTick)));
        }
    }
}
