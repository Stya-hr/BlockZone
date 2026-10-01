package dev.stya.blockzone.game.battlezone;

import com.mojang.blaze3d.vertex.PoseStack;
import com.ptcrys.fpsmatch.common.item.MapCreatorTool;
import dev.stya.blockzone.BlockZone;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BlockZone.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class BattlezoneWorldRendering {
    private static boolean mapCreatorPosePushed;

    private BattlezoneWorldRendering() {
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        BattlezoneClientRendering.renderWorld(event);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void pushMapCreatorAreaCameraTranslation(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SOLID_BLOCKS || !hasMapCreatorSelection()) {
            return;
        }

        Vec3 camera = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        poseStack.translate(-camera.x, -camera.y, -camera.z);
        mapCreatorPosePushed = true;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void popMapCreatorAreaCameraTranslation(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_SOLID_BLOCKS && mapCreatorPosePushed) {
            event.getPoseStack().popPose();
            mapCreatorPosePushed = false;
        }
    }

    private static boolean hasMapCreatorSelection() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return false;
        }
        return hasMapCreatorSelection(minecraft.player.getMainHandItem())
                || hasMapCreatorSelection(minecraft.player.getOffhandItem());
    }

    private static boolean hasMapCreatorSelection(ItemStack stack) {
        return stack.getItem() instanceof MapCreatorTool
                && MapCreatorTool.getBlockPos(stack, MapCreatorTool.BLOCK_POS_TAG_1) != null
                && MapCreatorTool.getBlockPos(stack, MapCreatorTool.BLOCK_POS_TAG_2) != null;
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        BattlezoneClientState.clear();
    }
}
