package dev.stya.blockzone.client.battlezone;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.stya.blockzone.BlockZone;
import dev.stya.blockzone.client.battlezone.model.AirborneModels;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** The route has one transport; descending players and their canopies remain visible to all teams. */
@Mod.EventBusSubscriber(modid = BlockZone.MOD_ID, value = Dist.CLIENT)
public final class RouteVisuals {
    private RouteVisuals() { }

    @SubscribeEvent
    public static void renderPlayer(RenderPlayerEvent.Pre event) {
        var pose = FlightVisualPose.get(event.getEntity());
        if (pose != null && pose.state == 1) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void renderAirborneModels(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
        var minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;
        var buffers = minecraft.renderBuffers().bufferSource();
        var type = RenderType.entityCutoutNoCull(TextureAtlas.LOCATION_BLOCKS);
        var vertices = buffers.getBuffer(type);
        var stack = event.getPoseStack();
        var camera = event.getCamera().getPosition();
        float partialTick = event.getPartialTick();
        double animationTime = minecraft.level.getGameTime() + partialTick;
        Vec3 transport = DeploymentVehicleState.position(partialTick);
        if (transport != null) {
            stack.pushPose();
            orientModel(stack, transport, camera, DeploymentVehicleState.yaw());
            int light = LevelRenderer.getLightColor(minecraft.level, BlockPos.containing(transport));
            AirborneModels.render(AirborneModels.AIRCRAFT, 8, stack, vertices, light);
            for (float side : new float[] {-4.25f, -2.25f, 2.25f, 4.25f}) {
                stack.pushPose();
                stack.translate(side, .625, -2.375);
                stack.mulPose(Axis.ZP.rotationDegrees((float) ((animationTime * 37 + side * 30) % 360)));
                AirborneModels.render(AirborneModels.PROPELLER, 8, stack, vertices, light);
                stack.popPose();
            }
            stack.popPose();
        }
        for (var player : minecraft.level.players()) {
            var pose = FlightVisualPose.get(player);
            if (pose == null || (pose.state != 2 && pose.state != 3) || player.isInvisible()) continue;
            var position = player.getPosition(partialTick);
            stack.pushPose();
            orientModel(stack, position, camera, Mth.rotLerp(partialTick, player.yBodyRotO, player.yBodyRot));
            int light = LevelRenderer.getLightColor(minecraft.level, BlockPos.containing(position.add(0, 2, 0)));
            stack.pushPose();
            float bodyPitch = pose.value(0, partialTick);
            stack.mulPose(Axis.XP.rotationDegrees(bodyPitch));
            AirborneModels.render(AirborneModels.PARACHUTE_PACK, 4, stack, vertices, light);
            stack.popPose();
            if (pose.state != 2) {
                stack.popPose();
                continue;
            }
            // The upright canopy follows the rotated pack's upper attachment point.
            double pitch = bodyPitch * Mth.DEG_TO_RAD;
            stack.translate(0, 1.45 * Math.cos(pitch) - .32 * Math.sin(pitch),
                    1.45 * Math.sin(pitch) + .32 * Math.cos(pitch));
            double swing = animationTime * .09 + player.getId();
            stack.mulPose(Axis.ZP.rotationDegrees((float) Math.sin(swing) * 2));
            stack.mulPose(Axis.XP.rotationDegrees((float) Math.cos(swing * .8) * 1.2f));
            light = LevelRenderer.getLightColor(minecraft.level, BlockPos.containing(position.add(0, 4, 0)));
            AirborneModels.renderParachute(4, stack, vertices, light, pose.appearance);
            stack.popPose();
        }
        buffers.endBatch(type);
        if (transport != null) {
            // Draw the luminous layers after opaque geometry; the pack owns all lamp positions.
            var glowType = RenderType.entityTranslucent(TextureAtlas.LOCATION_BLOCKS);
            stack.pushPose();
            orientModel(stack, transport, camera, DeploymentVehicleState.yaw());
            AirborneModels.render(AirborneModels.AIRCRAFT_LIGHTS, 8, stack,
                    buffers.getBuffer(glowType), LightTexture.FULL_BRIGHT);
            if (animationTime % 20 < 3) {
                AirborneModels.render(AirborneModels.AIRCRAFT_BEACON, 8, stack,
                        buffers.getBuffer(glowType), LightTexture.FULL_BRIGHT);
            }
            stack.popPose();
            buffers.endBatch(glowType);
        }
    }

    private static void orientModel(PoseStack stack, Vec3 position, Vec3 camera, float yaw) {
        stack.translate(position.x - camera.x, position.y - camera.y, position.z - camera.z);
        stack.mulPose(Axis.YP.rotationDegrees(180 - yaw));
        // Standard block/item models use positive Y for up and negative Z for forward.
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        var minecraft = Minecraft.getInstance();
        if (event.phase != TickEvent.Phase.END || minecraft.level == null) return;
        var position = DeploymentVehicleState.position(0);
        if (position == null) return;
        double yaw = Math.toRadians(DeploymentVehicleState.yaw());
        // One exhaust stream per engine, surviving even after the final passenger jumps.
        for (double side : new double[] {-4.25, -2.25, 2.25, 4.25}) {
            minecraft.level.addParticle(ParticleTypes.CLOUD,
                    position.x + Math.cos(yaw) * side + Math.sin(yaw) * 1.2,
                    position.y + .6,
                    position.z + Math.sin(yaw) * side - Math.cos(yaw) * 1.2,
                    Math.sin(yaw) * .08, 0, -Math.cos(yaw) * .08);
        }
    }
}
