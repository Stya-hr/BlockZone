package dev.stya.blockzone.map.battlezone;

/** Stable V formation in the route's horizontal plane. */
public final class FlightFormation {
    private FlightFormation() { }

    public static Offset offset(int slot, float routeYaw) {
        int row = (Math.max(0, slot) + 1) / 2;
        double side = slot <= 0 ? 0 : (slot % 2 == 1 ? -1 : 1) * row * 3.0;
        double back = row * 3.0;
        double yaw = Math.toRadians(routeYaw);
        return new Offset(Math.cos(yaw) * side + Math.sin(yaw) * back,
                Math.sin(yaw) * side - Math.cos(yaw) * back);
    }

    public record Offset(double x, double z) { }
}
