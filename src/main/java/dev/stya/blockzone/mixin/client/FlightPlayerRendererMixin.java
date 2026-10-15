package dev.stya.blockzone.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.stya.blockzone.client.battlezone.FlightVisualPose;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerRenderer.class)
public abstract class FlightPlayerRendererMixin {
    @Inject(method = "setupRotations", at = @At("HEAD"), cancellable = true)
    private void blockzone$rotateFlightBody(AbstractClientPlayer player, PoseStack stack,
            float age, float yaw, float partialTick, CallbackInfo callback) {
        var pose = FlightVisualPose.get(player);
        if (pose != null) {
            float heading = pose.state == 1 ? pose.routeYaw : Mth.rotLerp(partialTick, player.yRotO, player.getYRot());
            stack.mulPose(Axis.YP.rotationDegrees(180 - heading));
            stack.mulPose(Axis.ZP.rotationDegrees(pose.value(5, partialTick)));
            stack.mulPose(Axis.XP.rotationDegrees(pose.value(0, partialTick)));
            callback.cancel();
        }
    }
}
