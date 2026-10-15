package dev.stya.blockzone.client.battlezone;

import dev.stya.blockzone.BlockZone;
import dev.stya.blockzone.net.battlezone.FlightStateS2CPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Render-only poses: camera angles and collision dimensions remain independent. */
@Mod.EventBusSubscriber(modid = BlockZone.MOD_ID, value = Dist.CLIENT)
public final class FlightVisualPose {
    private static final Map<UUID, Blend> POSES = new HashMap<>();
    private static Object level;

    private FlightVisualPose() { }

    public static void apply(FlightStateS2CPacket packet) {
        Object currentLevel = Minecraft.getInstance().level;
        if (level != currentLevel) {
            POSES.clear();
            level = currentLevel;
        }
        if (packet.state() == 0) {
            POSES.remove(packet.playerId());
        } else {
            float[] target = switch (packet.state()) {
                case 1 -> new float[] {-90, -2.8f, .2f, 0, .05f, 0};
                case 2 -> new float[] {4, -2.4f, .18f, -.25f, .06f, 0};
                case 3 -> new float[] {-60, -.4f, 1.1f, .2f, .2f, 0};
                default -> null;
            };
            if (target != null) {
                var blend = POSES.computeIfAbsent(packet.playerId(), id -> new Blend(target));
                blend.target = target;
                blend.state = packet.state();
                blend.routeYaw = packet.routeYaw();
                blend.appearance = packet.appearance();
            }
        }
    }

    public static Blend get(Entity entity) {
        return entity.level() == level && entity.isAlive() && !entity.isSpectator()
                ? POSES.get(entity.getUUID()) : null;
    }

    @SubscribeEvent
    public static void entityLeaves(EntityLeaveLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            POSES.remove(event.getEntity().getUUID());
        }
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (Minecraft.getInstance().level != level) {
            POSES.clear();
            level = Minecraft.getInstance().level;
        }
        for (var entry : POSES.entrySet()) {
            Blend pose = entry.getValue();
            var player = Minecraft.getInstance().level == null ? null
                    : Minecraft.getInstance().level.getPlayerByUUID(entry.getKey());
            if (player != null && (pose.state == 2 || pose.state == 3)) {
                var velocity = player == Minecraft.getInstance().player ? player.getDeltaMovement()
                        : player.position().subtract(player.xo, player.yo, player.zo);
                double yaw = player.getYRot() * Mth.DEG_TO_RAD;
                double speed = pose.state == 2 ? .08 : .05;
                float forward = (float) Mth.clamp((-Math.sin(yaw) * velocity.x + Math.cos(yaw) * velocity.z) / speed, -1, 1);
                float left = (float) Mth.clamp((Math.cos(yaw) * velocity.x + Math.sin(yaw) * velocity.z) / speed, -1, 1);
                pose.target[0] = pose.state == 2 ? 4 + forward * 24 : -60 - forward * 14;
                pose.target[5] = left * (pose.state == 2 ? 28 : 16);
                if (pose.state == 2) {
                    pose.target[1] = -2.4f + forward * .2f;
                    pose.target[3] = -.25f - forward * .3f;
                }
            }
            for (int i = 0; i < pose.current.length; i++) {
                pose.previous[i] = pose.current[i];
                pose.current[i] = Mth.lerp(.14f, pose.current[i], pose.target[i]);
            }
        }
    }

    public static final class Blend {
        public int state;
        public float routeYaw;
        public int appearance;
        private float[] target;
        private final float[] current;
        private final float[] previous;

        private Blend(float[] target) {
            this.target = target;
            current = target.clone();
            previous = target.clone();
        }

        public float value(int index, float partialTick) {
            return Mth.lerp(partialTick, previous[index], current[index]);
        }
    }
}
