package dev.stya.blockzone.client.loot;

import com.tacz.guns.api.item.IGun;
import com.tacz.guns.client.input.InteractKey;
import dev.stya.blockzone.BlockZone;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/** Supplies TaCZ's normal use action when its interaction key is pressed without a gun equipped. */
@Mod.EventBusSubscriber(modid = BlockZone.MOD_ID, value = Dist.CLIENT)
public final class TaczUnarmedInteraction {
    private TaczUnarmedInteraction() { }

    @SubscribeEvent
    public static void key(InputEvent.Key event) {
        var mc = Minecraft.getInstance();
        if (event.getAction() == GLFW.GLFW_PRESS
                && InteractKey.INTERACT_KEY.matches(event.getKey(), event.getScanCode())
                && !mc.options.keyUse.matches(event.getKey(), event.getScanCode())) {
            useWithoutGun(mc);
        }
    }

    @SubscribeEvent
    public static void mouse(InputEvent.MouseButton.Post event) {
        var mc = Minecraft.getInstance();
        if (event.getAction() == GLFW.GLFW_PRESS && InteractKey.INTERACT_KEY.matchesMouse(event.getButton())
                && !mc.options.keyUse.matchesMouse(event.getButton())) {
            useWithoutGun(mc);
        }
    }

    private static void useWithoutGun(Minecraft mc) {
        if (mc.player != null && mc.gameMode != null && mc.level != null && mc.screen == null
                && !mc.player.isSpectator() && !IGun.mainHandHoldGun(mc.player)) {
            if (mc.hitResult instanceof EntityHitResult entity) {
                mc.gameMode.interact(mc.player, entity.getEntity(), InteractionHand.MAIN_HAND);
            } else if (mc.hitResult instanceof BlockHitResult block && block.getType() == HitResult.Type.BLOCK) {
                mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, block);
            } else {
                mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
            }
        }
    }
}
