package dev.stya.blockzone.mixin.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerModel.class)
public abstract class PlatingPlayerModelMixin<T extends LivingEntity> extends HumanoidModel<T> {
    protected PlatingPlayerModelMixin(ModelPart root) { super(root); }
    @Inject(method = "setupAnim", at = @At("TAIL"))
    private void blockzone$plating(T entity, float swing, float amount, float age, float yaw, float pitch, CallbackInfo ci) {
        if (!entity.isUsingItem() || !entity.getUseItem().is(dev.stya.blockzone.registry.BlockzoneItems.ARMOR_PLATE.get())
                || dev.stya.blockzone.client.battlezone.FlightVisualPose.get(entity) != null) return;
        float progress = (entity.getTicksUsingItem() + net.minecraft.client.Minecraft.getInstance().getFrameTime()) / 40F;
        float insert = net.minecraft.util.Mth.clamp((progress - .5F) / .3F, 0, 1);
        rightArm.xRot = leftArm.xRot = -1.1F + insert * .4F;
        rightArm.yRot = -.4F;
        leftArm.yRot = .4F;
        rightArm.zRot = .12F;
        leftArm.zRot = -.12F;
        var model = (PlayerModel<?>)(Object)this;
        model.rightSleeve.copyFrom(rightArm);
        model.leftSleeve.copyFrom(leftArm);
    }
}
