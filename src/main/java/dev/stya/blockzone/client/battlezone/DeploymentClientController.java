package dev.stya.blockzone.client.battlezone;

import com.mojang.blaze3d.platform.InputConstants;
import dev.stya.blockzone.BlockZone;
import dev.stya.blockzone.deployment.ParachuteMotion;
import dev.stya.blockzone.mixin.client.OptionsCameraTypeAccessor;
import dev.stya.blockzone.net.BlockzoneNetwork;
import dev.stya.blockzone.net.battlezone.FlightStateS2CPacket;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Pose;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = BlockZone.MOD_ID, value = Dist.CLIENT)
public final class DeploymentClientController {
    private static final KeyMapping RELEASE = new KeyMapping("key.blockzone.release_deployment",
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, "key.categories.blockzone.gameplay");
    private static final KeyMapping PARACHUTE = new KeyMapping("key.blockzone.toggle_parachute",
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_SPACE, "key.categories.blockzone.gameplay");
    private static int state;
    private static int sentForward, sentLeft, inputTicks;
    public static boolean isAirborne() { return state != 0; }
    public static int state() { return state; }
    public static KeyMapping releaseKey() { return RELEASE; }
    public static KeyMapping parachuteKey() { return PARACHUTE; }
    private static float cameraDistance = 4;
    private static float previousCameraDistance = 4;

    public static double cameraDistance(double vanillaDistance, float partialTick) {
        if (state == 0) return vanillaDistance;
        return net.minecraft.util.Mth.lerp(partialTick, previousCameraDistance, cameraDistance);
    }
    private static net.minecraft.client.player.LocalPlayer trackedPlayer;
    private static Pose previousPose;
    private static boolean previousNoGravity;
    private static CameraType previousCameraType;

    private DeploymentClientController() { }

    @SubscribeEvent
    public static void movementInput(MovementInputUpdateEvent event) {
        if ((state == 2 || state == 3) && event.getEntity() == trackedPlayer) {
            // Keys are sampled separately; vanilla air acceleration must not add a second steering force.
            event.getInput().forwardImpulse = event.getInput().leftImpulse = 0;
            event.getInput().jumping = false;
        }
    }

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
        if (packet.state() == 1) {
            var minecraft = Minecraft.getInstance();
            if (minecraft.screen instanceof dev.stya.blockzone.client.inventory.InventoryPanelScreen
                    || minecraft.screen instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?>) {
                player.closeContainer();
            }
        }
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
        sentForward = sentLeft = inputTicks = 0;
        cameraDistance = previousCameraDistance = 4;
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
        previousCameraDistance = cameraDistance;
        cameraDistance = net.minecraft.util.Mth.lerp(.2f, cameraDistance, state == 1 ? 18 : state == 2 ? 7 : 4);
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.player != trackedPlayer
                || minecraft.player.isDeadOrDying() || minecraft.player.isSpectator()) {
            clear();
        }
        while (RELEASE.consumeClick()) {
            if (state == 1 && minecraft.screen == null) {
                BlockzoneNetwork.sendToServer(new dev.stya.blockzone.net.battlezone.ReleaseDeploymentC2SPacket());
            }
        }
        while (PARACHUTE.consumeClick()) {
            if ((state == 2 || state == 3) && minecraft.screen == null) {
                BlockzoneNetwork.sendToServer(new dev.stya.blockzone.net.battlezone.ToggleParachuteC2SPacket());
            }
        }
        if (state != 0 && trackedPlayer != null) {
            trackedPlayer.setForcedPose(Pose.SWIMMING);
            trackedPlayer.setNoGravity(true);
            if (state == 1) {
                trackedPlayer.setDeltaMovement(0, 0, 0);
            } else {
                trackedPlayer.getAbilities().flying = false;
                var options = minecraft.options;
                int forward = minecraft.screen == null ? (options.keyUp.isDown() ? 1 : 0) - (options.keyDown.isDown() ? 1 : 0) : 0;
                int left = minecraft.screen == null ? (options.keyLeft.isDown() ? 1 : 0) - (options.keyRight.isDown() ? 1 : 0) : 0;
                if (forward != sentForward || left != sentLeft || ++inputTicks >= 10) {
                    BlockzoneNetwork.sendToServer(new dev.stya.blockzone.net.battlezone.DescentInputC2SPacket(forward, left));
                    sentForward = forward; sentLeft = left; inputTicks = 0;
                }
                var movement = trackedPlayer.getDeltaMovement();
                var motion = state == 2
                        ? ParachuteMotion.step(movement.x, movement.y, movement.z,
                                trackedPlayer.getYRot(), trackedPlayer.getXRot(), forward, left)
                        : ParachuteMotion.freefall(movement.x, movement.y, movement.z, trackedPlayer.getYRot(), forward, left);
                trackedPlayer.setDeltaMovement(motion.x(), motion.y(), motion.z());
            }
        }
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
