package dev.stya.blockzone.client.battlezone;

import dev.stya.blockzone.map.battlezone.ParachuteMotion;
import dev.stya.blockzone.mixin.client.OptionsCameraTypeAccessor;
import dev.stya.blockzone.net.battlezone.FlightStateS2CPacket;
import dev.stya.blockzone.net.battlezone.BattlezoneNetwork;
import com.mojang.blaze3d.platform.InputConstants;
import dev.stya.blockzone.BlockZone;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Pose;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = BlockZone.MOD_ID, value = Dist.CLIENT)
public final class DeploymentClientController {
    private static final KeyMapping RELEASE = new KeyMapping("key.blockzone.release_deployment",
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, "key.categories.blockzone");
    private static final KeyMapping PARACHUTE = new KeyMapping("key.blockzone.toggle_parachute",
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_H, "key.categories.blockzone");
    private static int state;
    private static net.minecraft.client.player.LocalPlayer trackedPlayer;
    private static Pose previousPose;
    private static boolean previousNoGravity;
    private static CameraType previousCameraType;

    private DeploymentClientController() { }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (state == 0 || minecraft.screen != null || event.getAction() != GLFW.GLFW_PRESS
                || !minecraft.options.keyTogglePerspective.matches(event.getKey(), event.getScanCode())) {
            return;
        }
        minecraft.options.keyTogglePerspective.consumeClick();
        setCameraType(minecraft, minecraft.options.getCameraType().cycle());
    }

    public static void apply(FlightStateS2CPacket packet) {
        FlightVisualPose.apply(packet);
        var player = Minecraft.getInstance().player;
        if (player == null) {
            clear();
            return;
        }
        if (!packet.playerId().equals(player.getUUID())) {
            return;
        }
        if (packet.state() == 0 && trackedPlayer != player) {
            return;
        }
        if (trackedPlayer != player) {
            clear();
            trackedPlayer = player;
            previousPose = player.getForcedPose();
            previousNoGravity = player.isNoGravity();
            Minecraft minecraft = Minecraft.getInstance();
            previousCameraType = minecraft.options.getCameraType();
            setCameraType(minecraft, CameraType.THIRD_PERSON_BACK);
        }
        int previous = state;
        state = packet.state();
        if (state == 3 && previous < 2) {
            player.setDeltaMovement(0, -0.8, 0);
        }
        if (state == 0) {
            clear();
        }
    }

    private static void clear() {
        if (trackedPlayer != null) {
            trackedPlayer.setForcedPose(previousPose);
            trackedPlayer.setNoGravity(previousNoGravity);
            Minecraft minecraft = Minecraft.getInstance();
            if (previousCameraType != null) {
                setCameraType(minecraft, previousCameraType);
            }
        }
        trackedPlayer = null;
        previousPose = null;
        previousCameraType = null;
        state = 0;
    }

    private static void setCameraType(Minecraft minecraft, CameraType cameraType) {
        ((OptionsCameraTypeAccessor) (Object) minecraft.options).blockzone$setCameraType(cameraType);
        minecraft.options.save();
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
        while (PARACHUTE.consumeClick()) {
            if ((state == 2 || state == 3) && minecraft.screen == null) {
                BattlezoneNetwork.toggleParachute();
            }
        }
        if (state != 0 && trackedPlayer != null) {
            trackedPlayer.setForcedPose(Pose.SWIMMING);
            trackedPlayer.setNoGravity(true);
            if (state == 1) {
                trackedPlayer.setDeltaMovement(0, 0, 0);
            } else {
                trackedPlayer.getAbilities().flying = false;
                var movement = trackedPlayer.getDeltaMovement();
                var motion = state == 2
                        ? ParachuteMotion.step(movement.x, movement.y, movement.z,
                                trackedPlayer.getYRot(), trackedPlayer.getXRot())
                        : ParachuteMotion.freefall(movement.x, movement.y, movement.z, trackedPlayer.getYRot());
                trackedPlayer.setDeltaMovement(motion.x(), motion.y(), motion.z());
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
                : Component.translatable(state == 2 ? "blockzone.deployment.close_hint" : "blockzone.deployment.open_hint",
                        PARACHUTE.getTranslatedKeyMessage());
        event.getGuiGraphics().drawCenteredString(minecraft.font, text,
                event.getWindow().getGuiScaledWidth() / 2, event.getWindow().getGuiScaledHeight() - 65, 0xFFFFFF);
    }

    @Mod.EventBusSubscriber(modid = BlockZone.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        private Registration() { }

        @SubscribeEvent
        public static void registerKeys(RegisterKeyMappingsEvent event) {
            event.register(RELEASE);
            event.register(PARACHUTE);
        }
    }
}
