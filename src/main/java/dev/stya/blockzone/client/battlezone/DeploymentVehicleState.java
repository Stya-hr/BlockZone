package dev.stya.blockzone.client.battlezone;

import dev.stya.blockzone.deployment.FlightRoute;
import dev.stya.blockzone.net.battlezone.DeploymentVehicleS2CPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

/** Transport state survives passengers jumping and is cleared by the deployment phase snapshot. */
public final class DeploymentVehicleState {
    private static DeploymentVehicleS2CPacket snapshot;
    private static Object level;
    private static long receivedAt;

    private DeploymentVehicleState() { }

    public static void apply(DeploymentVehicleS2CPacket packet) {
        var current = Minecraft.getInstance().level;
        if (current == null || !current.dimension().location().equals(packet.dimension())) {
            clear();
            return;
        }
        snapshot = packet.route() == null ? null : packet;
        level = current;
        receivedAt = current.getGameTime();
    }

    public static void clear() {
        snapshot = null;
        level = null;
    }

    static FlightRoute route() {
        var current = Minecraft.getInstance().level;
        if (current != level) clear();
        return snapshot == null ? null : snapshot.route();
    }

    static Vec3 position(float partialTick) {
        var route = route();
        if (route == null) return null;
        double ticks = snapshot.elapsedTicks() + Minecraft.getInstance().level.getGameTime() - receivedAt + partialTick;
        var point = route.vehiclePosition(ticks);
        return new Vec3(point.x(), route.startY() + 1.5, point.z());
    }

    static float yaw() {
        var route = route();
        return route == null ? 0 : (float) Math.toDegrees(Math.atan2(
                -(route.endX() - route.startX()), route.endZ() - route.startZ()));
    }
}
