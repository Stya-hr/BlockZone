package dev.stya.blockzone.util.battlezone;

/** Shared full sphere, centred at the map's lowest Y. */
public record BattlezoneZoneGeometry(double centerX, double centerY, double centerZ, double radius) {
    public static double centerY(int y1, int y2) {
        return Math.min(y1, y2);
    }

    public boolean contains(double x, double y, double z) {
        if (!(radius > 0.0)) {
            return false;
        }
        double dx = x - centerX;
        double dy = y - centerY;
        double dz = z - centerZ;
        return dx * dx + dy * dy + dz * dz <= radius * radius;
    }
}
