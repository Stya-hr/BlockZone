package dev.stya.blockzone.util.battlezone;

/** Shared safe-zone geometry; radius is the square prism's half side length. */
public record ZoneGeometry(double centerX, double centerY, double centerZ, double radius, ZoneShape shape) {
    public static final double VISUAL_HEIGHT = 8.0;
    public ZoneGeometry(double centerX, double centerY, double centerZ, double radius) {
        this(centerX, centerY, centerZ, radius, ZoneShape.CYLINDER);
    }
    /** Translate the horizontal footprint into the map, preserving its radius. */
    public ZoneGeometry fitInside(BoundaryGeometry bounds) {
        if (!Double.isFinite(radius) || radius < 0 || radius * 2 > Math.min(bounds.maxX() - bounds.minX(), bounds.maxZ() - bounds.minZ())) {
            throw new IllegalArgumentException("Circle radius cannot fit inside map bounds");
        }
        double x = Double.isFinite(centerX) ? centerX : (bounds.minX() + bounds.maxX()) / 2;
        double z = Double.isFinite(centerZ) ? centerZ : (bounds.minZ() + bounds.maxZ()) / 2;
        return new ZoneGeometry(Math.max(bounds.minX() + radius, Math.min(bounds.maxX() - radius, x)),
                centerY, Math.max(bounds.minZ() + radius, Math.min(bounds.maxZ() - radius, z)), radius, shape);
    }

    public static double centerY(int y1, int y2) {
        return Math.min(y1, y2);
    }

    public boolean contains(double x, double y, double z) {
        if (!(radius > 0.0)) {
            return false;
        }
        return containsHorizontal(x, z);
    }

    /** Horizontal projection used by deployment, independent of height. */
    public boolean containsHorizontal(double x, double z) {
        if (!(radius > 0.0)) return false;
        double dx = x - centerX;
        double dz = z - centerZ;
        if (shape == ZoneShape.SQUARE_PRISM) return Math.abs(dx) <= radius && Math.abs(dz) <= radius;
        return dx * dx + dz * dz <= radius * radius;
    }
}
