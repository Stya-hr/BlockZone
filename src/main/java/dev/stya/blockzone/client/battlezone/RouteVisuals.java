package dev.stya.blockzone.client.battlezone;

import dev.stya.blockzone.BlockZone;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Route visibility is local to the observer; airborne opponents render normally. */
@Mod.EventBusSubscriber(modid = BlockZone.MOD_ID, value = Dist.CLIENT)
public final class RouteVisuals {
    private RouteVisuals() { }

    @SubscribeEvent
    public static void renderPlayer(RenderPlayerEvent.Pre event) {
        var pose = FlightVisualPose.get(event.getEntity());
        var local = Minecraft.getInstance().player;
        if (pose != null && pose.state == 1 && local != null
                && !pose.squad.contains(local.getUUID())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        var minecraft = Minecraft.getInstance();
        if (event.phase != TickEvent.Phase.END || minecraft.level == null || minecraft.player == null) return;
        for (var player : minecraft.level.players()) {
            var pose = FlightVisualPose.get(player);
            if (pose == null || pose.state != 1 || !pose.squad.contains(minecraft.player.getUUID())) continue;
            double yaw = Math.toRadians(pose.routeYaw);
            // Emit behind the fixed route heading, independently of mouse look.
            for (int i = 0; i < 3; i++) {
                double distance = 1.2 + i * .45;
                minecraft.level.addParticle(ParticleTypes.CLOUD,
                        player.getX() + Math.sin(yaw) * distance, player.getY() + .25,
                        player.getZ() - Math.cos(yaw) * distance,
                        Math.sin(yaw) * .08, 0, -Math.cos(yaw) * .08);
            }
        }
    }
}
