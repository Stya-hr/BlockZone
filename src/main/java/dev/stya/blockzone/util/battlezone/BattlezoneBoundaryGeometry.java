package dev.stya.blockzone.util.battlezone;

/** Horizontal map boundary shared by collision enforcement and contact rendering. */
public record BattlezoneBoundaryGeometry(double minX, double maxX, double minZ, double maxZ) {
    static final double CONTACT_EPSILON = 0.04;

    public static BattlezoneBoundaryGeometry of(int x1, int z1, int x2, int z2) {
        return new BattlezoneBoundaryGeometry(Math.min(x1, x2), Math.max(x1, x2) + 1.0,
                Math.min(z1, z2), Math.max(z1, z2) + 1.0);
    }

    public boolean touchesX(double plane, double boxMinX, double boxMaxX, double boxMinZ, double boxMaxZ) {
        return touchesPlane(plane, boxMinX, boxMaxX) && boxMaxZ >= minZ && boxMinZ <= maxZ;
    }

    public boolean touchesZ(double plane, double boxMinX, double boxMaxX, double boxMinZ, double boxMaxZ) {
        return touchesPlane(plane, boxMinZ, boxMaxZ) && boxMaxX >= minX && boxMinX <= maxX;
    }

    private static boolean touchesPlane(double plane, double min, double max) {
        return min <= plane + CONTACT_EPSILON && max >= plane - CONTACT_EPSILON;
    }
}
