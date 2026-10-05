package dev.stya.blockzone.client.equipment;

import com.mojang.math.Axis;
import dev.stya.blockzone.BlockZone;
import dev.stya.blockzone.registry.BlockzoneItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BlockZone.MOD_ID, value = Dist.CLIENT)
public final class PlatingAnimation {
    private PlatingAnimation() { }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;
        while (EquipmentClientEvents.DETACH.consumeClick()) dev.stya.blockzone.net.battlezone.BattlezoneNetwork.detachArmor();
    }
    @SubscribeEvent public static void hand(RenderHandEvent event) {
        var mc = Minecraft.getInstance();
        var player = mc.player;
        if (player == null || !player.isUsingItem() || !player.getUseItem().is(BlockzoneItems.ARMOR_PLATE.get())) return;
        event.setCanceled(true);
        if (event.getHand() != player.getUsedItemHand()) return;
        float progress = Math.min(1, (player.getTicksUsingItem() + event.getPartialTick()) / 40F);
        float enter = (float) net.minecraft.util.Mth.smoothstep(Math.min(1, progress / .2F));
        float insert = (float) net.minecraft.util.Mth.smoothstep(net.minecraft.util.Mth.clamp((progress - .5F) / .3F, 0, 1));
        float exit = (float) net.minecraft.util.Mth.smoothstep(net.minecraft.util.Mth.clamp((progress - .85F) / .15F, 0, 1));
        float handed = player.getMainArm() == net.minecraft.world.entity.HumanoidArm.RIGHT ? 1 : -1;
        if (player.getUsedItemHand() == InteractionHand.OFF_HAND) handed = -handed;
        var pose = event.getPoseStack();
        pose.pushPose();
        pose.translate(handed * .23F, -.55F + enter * .18F - insert * .22F - exit * .45F, -.7F);
        pose.mulPose(Axis.XP.rotationDegrees(-12 - insert * 15));
        pose.mulPose(Axis.YP.rotationDegrees(handed * -12));
        pose.scale(.7F, .7F, .7F);
        mc.getItemRenderer().renderStatic(player.getUseItem(), ItemDisplayContext.FIXED, event.getPackedLight(),
                net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, pose, event.getMultiBufferSource(), player.level(), 0);
        pose.popPose();
        if (mc.getEntityRenderDispatcher().getRenderer(player) instanceof PlayerRenderer renderer) {
            for (boolean right : new boolean[]{true, false}) {
                pose.pushPose();
                pose.translate(right ? .35F : -.35F, -.65F + enter * .12F - insert * .12F - exit * .35F, -.65F);
                pose.mulPose(Axis.XP.rotationDegrees(-50));
                pose.mulPose(Axis.YP.rotationDegrees(right ? -30 : 30));
                pose.scale(.8F, .8F, .8F);
                if (right) renderer.renderRightHand(pose, event.getMultiBufferSource(), event.getPackedLight(), player);
                else renderer.renderLeftHand(pose, event.getMultiBufferSource(), event.getPackedLight(), player);
                pose.popPose();
            }
        }
    }
}
