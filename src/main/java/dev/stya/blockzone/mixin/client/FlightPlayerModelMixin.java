package dev.stya.blockzone.mixin.client;

import dev.stya.blockzone.client.battlezone.FlightVisualPose;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerModel.class)
public abstract class FlightPlayerModelMixin<T extends LivingEntity> extends HumanoidModel<T> {
    protected FlightPlayerModelMixin(ModelPart root) { super(root); }

    @Inject(method = "setupAnim", at = @At("TAIL"))
    private void blockzone$flightLimbs(T player, float swing, float amount, float age,
            float yaw, float pitch, CallbackInfo callback) {
        var pose = FlightVisualPose.get(player);
        if (pose == null) return;
        float partialTick = Minecraft.getInstance().getFrameTime();
        body.xRot = body.yRot = body.zRot = 0;
        head.xRot = pose.state == 1 ? 0 : Mth.clamp(pitch * Mth.DEG_TO_RAD, -.35f, .35f);
        head.yRot = pose.state == 1 ? 0 : Mth.clamp(yaw * Mth.DEG_TO_RAD, -.8f, .8f);
        head.zRot = 0;
        hat.copyFrom(head);
        leftArm.xRot = rightArm.xRot = pose.value(1, partialTick);
        leftArm.yRot = rightArm.yRot = 0;
        leftArm.zRot = -pose.value(2, partialTick);
        rightArm.zRot = pose.value(2, partialTick);
        leftLeg.xRot = rightLeg.xRot = pose.value(3, partialTick);
        leftLeg.yRot = rightLeg.yRot = 0;
        leftLeg.zRot = -pose.value(4, partialTick);
        rightLeg.zRot = pose.value(4, partialTick);
        var model = (PlayerModel<?>)(Object)this;
        model.leftSleeve.copyFrom(leftArm);
        model.rightSleeve.copyFrom(rightArm);
        model.leftPants.copyFrom(leftLeg);
        model.rightPants.copyFrom(rightLeg);
        model.jacket.copyFrom(body);
    }
}
