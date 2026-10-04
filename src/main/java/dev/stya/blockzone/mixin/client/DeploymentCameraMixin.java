package dev.stya.blockzone.mixin.client;

import dev.stya.blockzone.client.battlezone.DeploymentClientController;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(Camera.class)
public abstract class DeploymentCameraMixin {
    @ModifyArg(method = "setup", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/Camera;getMaxZoom(D)D"), index = 0)
    private double blockzone$routeDistance(double vanillaDistance) {
        var camera = (Camera)(Object)this;
        return camera.getEntity() == Minecraft.getInstance().player
                ? DeploymentClientController.cameraDistance(vanillaDistance, Minecraft.getInstance().getFrameTime()) : vanillaDistance;
    }
}
