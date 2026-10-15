package dev.stya.blockzone.mixin.client;

import dev.stya.blockzone.client.battlezone.DeploymentClientController;
import dev.stya.blockzone.client.battlezone.DeploymentVehicleState;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;

@Mixin(Camera.class)
public abstract class DeploymentCameraMixin {
    @Shadow protected abstract void setPosition(double x, double y, double z);

    @Inject(method = "setup", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/Camera;setPosition(DDD)V", ordinal = 0, shift = At.Shift.AFTER))
    private void blockzone$routePosition(BlockGetter level, Entity entity,
                                        boolean detached, boolean reverse, float partialTick, CallbackInfo ci) {
        if (entity != Minecraft.getInstance().player || DeploymentClientController.state() != 1) return;
        var transport = DeploymentVehicleState.position(partialTick);
        if (transport == null) return;
        // Preserve the eye-height offset and mouse look, but use the aircraft's continuous motion.
        double eyeHeight = ((Camera)(Object)this).getPosition().y
                - net.minecraft.util.Mth.lerp(partialTick, entity.yo, entity.getY());
        setPosition(transport.x, transport.y - 1.5 + eyeHeight, transport.z);
    }

    @ModifyArg(method = "setup", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/Camera;getMaxZoom(D)D"), index = 0)
    private double blockzone$routeDistance(double vanillaDistance) {
        var camera = (Camera)(Object)this;
        return camera.getEntity() == Minecraft.getInstance().player
                ? DeploymentClientController.cameraDistance(vanillaDistance, Minecraft.getInstance().getFrameTime()) : vanillaDistance;
    }
}
