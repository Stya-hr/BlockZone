package dev.stya.blockzone.game.battlezone;

import com.mojang.blaze3d.platform.InputConstants;
import dev.stya.blockzone.BlockZone;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Pose;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = BlockZone.MOD_ID, value = Dist.CLIENT)
public final class BattlezoneDeploymentClient {
    private static final KeyMapping RELEASE = new KeyMapping("key.blockzone.release_deployment",
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, "key.categories.blockzone");
    private static int state;
    private static net.minecraft.client.player.LocalPlayer trackedPlayer;
    private static Pose previousPose;

    private BattlezoneDeploymentClient() { }

    static void apply(BattlezoneFlightStateS2CPacket packet) {
        var player = Minecraft.getInstance().player;
        if (player == null) {
            clear();
            return;
        }
        if (trackedPlayer != player) {
            clear();
            trackedPlayer = player;
            previousPose = player.getForcedPose();
        }
        state = packet.state();
        if (state == 0) {
            clear();
        }
    }

    private static void clear() {
        if (trackedPlayer != null) {
            trackedPlayer.setForcedPose(previousPose);
        }
        trackedPlayer = null;
        previousPose = null;
        state = 0;
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.player != trackedPlayer
                || minecraft.player.isDeadOrDying() || minecraft.player.isSpectator()) {
            clear();
        }
        while (RELEASE.consumeClick()) {
            if (state == 1 && minecraft.screen == null) {
                BattlezoneNetwork.releaseDeployment();
            }
        }
        if (state != 0 && trackedPlayer != null) {
            trackedPlayer.setForcedPose(Pose.SWIMMING);
            if (state == 1) {
                trackedPlayer.setDeltaMovement(0, 0, 0);
            }
        }
    }

    @SubscribeEvent
    public static void overlay(RenderGuiOverlayEvent.Post event) {
        if (state == 0 || event.getOverlay() != VanillaGuiOverlay.HOTBAR.type()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        Component text = state == 1
                ? Component.translatable("blockzone.deployment.release_hint", RELEASE.getTranslatedKeyMessage())
                : Component.translatable("blockzone.deployment.diving_hint");
        event.getGuiGraphics().drawCenteredString(minecraft.font, text,
                event.getWindow().getGuiScaledWidth() / 2, event.getWindow().getGuiScaledHeight() - 65, 0xFFFFFF);
    }

    @Mod.EventBusSubscriber(modid = BlockZone.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        private Registration() { }

        @SubscribeEvent
        public static void registerKeys(RegisterKeyMappingsEvent event) {
            event.register(RELEASE);
        }
    }
}
