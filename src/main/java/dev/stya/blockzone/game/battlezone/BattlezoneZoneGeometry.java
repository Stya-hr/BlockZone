package dev.stya.blockzone.game.battlezone;

/** Shared full sphere, centred at the map's lowest Y. */
record BattlezoneZoneGeometry(double centerX, double centerY, double centerZ, double radius) {
    static double centerY(int y1, int y2) {
        return Math.min(y1, y2);
    }

    boolean contains(double x, double y, double z) {
        if (!(radius > 0.0)) {
            return false;
        }
        double dx = x - centerX;
        double dy = y - centerY;
        double dz = z - centerZ;
        return dx * dx + dy * dy + dz * dz <= radius * radius;
    }
}
