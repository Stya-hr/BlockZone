package dev.stya.blockzone.client.battlezone;

import dev.stya.blockzone.deployment.FlightRoute;
import dev.stya.blockzone.deployment.FlightTimeline;
import dev.stya.blockzone.net.battlezone.DeploymentVehicleS2CPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

/** Transport state survives passengers jumping and is cleared by the deployment phase snapshot. */
public final class DeploymentVehicleState {
    private static DeploymentVehicleS2CPacket snapshot;
    private static Object level;
    private static final FlightTimeline TIMELINE = new FlightTimeline();

    private DeploymentVehicleState() { }

    public static void apply(DeploymentVehicleS2CPacket packet) {
        var current = Minecraft.getInstance().level;
        if (current == null || !current.dimension().location().equals(packet.dimension())) {
            clear();
            return;
        }
        boolean reset = current != level || snapshot == null || !java.util.Objects.equals(snapshot.route(), packet.route())
                || packet.elapsedTicks() < snapshot.elapsedTicks();
        TIMELINE.update(packet.elapsedTicks(), current.getGameTime() + Minecraft.getInstance().getFrameTime(), reset);
        snapshot = packet.route() == null ? null : packet;
        level = current;
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

    public static double speed() {
        var route = route();
        return route == null ? Double.NaN : route.speed();
    }

    public static Vec3 position(float partialTick) {
        var route = route();
        if (route == null) return null;
        double ticks = TIMELINE.ticks(Minecraft.getInstance().level.getGameTime() + partialTick);
        var point = route.vehiclePosition(ticks);
        return new Vec3(point.x(), route.startY() + 1.5, point.z());
    }

    static float yaw() {
        var route = route();
        return route == null ? 0 : (float) Math.toDegrees(Math.atan2(
                -(route.endX() - route.startX()), route.endZ() - route.startZ()));
    }
}
