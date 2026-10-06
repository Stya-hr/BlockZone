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
                case 1 -> new float[] {-90, -2.8f, .2f, 0, .05f};
                case 2 -> new float[] {12, -2.4f, .18f, -.45f, .06f};
                case 3 -> new float[] {-60, -.4f, 1.1f, .2f, .2f};
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
        for (Blend pose : POSES.values()) {
            for (int i = 0; i < pose.current.length; i++) {
                pose.previous[i] = pose.current[i];
                pose.current[i] = Mth.lerp(.35f, pose.current[i], pose.target[i]);
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
