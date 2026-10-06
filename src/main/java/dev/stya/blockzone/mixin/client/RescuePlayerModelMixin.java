package dev.stya.blockzone.mixin.client;

import dev.stya.blockzone.client.battlezone.RescueClientState;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Low collision dimensions with a face-up injured pose and slow dragging motions. */
@Mixin(PlayerModel.class)
public abstract class RescuePlayerModelMixin<T extends LivingEntity> extends HumanoidModel<T> {
    protected RescuePlayerModelMixin(ModelPart root) { super(root); }
    @Inject(method = "setupAnim", at = @At("TAIL"))
    private void blockzone$rescue(T player, float swing, float amount, float age,
                                 float yaw, float pitch, CallbackInfo ci) {
        int state = RescueClientState.state(player);
        if (state == 0) return;
        float crawl = Mth.sin(swing * .6F) * Math.min(amount, .2F);
        float breathe = Mth.sin(age * .09F) * .035F;
        if (state == 1) {
            head.xRot = Mth.clamp(pitch * Mth.DEG_TO_RAD * .15F, -.15F, .15F);
            head.zRot = 0;
            head.yRot = Mth.clamp(yaw * Mth.DEG_TO_RAD, -.6F, .6F);
            body.xRot = body.yRot = body.zRot = 0;
            rightArm.xRot = -.35F + crawl + breathe;
            leftArm.xRot = -.55F - crawl + breathe;
            rightArm.yRot = -.2F; leftArm.yRot = .2F;
            rightArm.zRot = .2F; leftArm.zRot = -.2F;
            rightLeg.xRot = .25F - crawl; leftLeg.xRot = .35F + crawl;
            rightLeg.yRot = leftLeg.yRot = 0;
            rightLeg.zRot = .12F; leftLeg.zRot = -.12F;
        } else {
            body.xRot = .35F;
            head.xRot = .45F;
            rightLeg.xRot = -1.15F; leftLeg.xRot = -.35F;
            rightLeg.yRot = leftLeg.yRot = 0;
            rightLeg.zRot = leftLeg.zRot = 0;
            float work = Mth.sin(age * .35F) * .12F;
            rightArm.xRot = -1.05F + work; leftArm.xRot = -1.05F - work;
            rightArm.yRot = -.3F; leftArm.yRot = .3F;
            rightArm.zRot = .05F; leftArm.zRot = -.05F;
        }
        hat.copyFrom(head);
        var model = (PlayerModel<?>)(Object)this;
        model.rightSleeve.copyFrom(rightArm); model.leftSleeve.copyFrom(leftArm);
        model.rightPants.copyFrom(rightLeg); model.leftPants.copyFrom(leftLeg); model.jacket.copyFrom(body);
    }
}
