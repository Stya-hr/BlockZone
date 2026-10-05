package dev.stya.blockzone.zone;



/** Distances use the player's collision box; ribbon ends move without shifting the lettering. */
public final class WarningRibbonGeometry {
    public static final double REVEAL_DISTANCE = 2.0;
    public static final double HALF_LENGTH = 12.0;
    public static final double END_FADE = 2.0;
    public static final double HEIGHT = 0.60;

    private WarningRibbonGeometry() {}

    public static float proximityAlpha(double plane, double boxMin, double boxMax) {
        double gap = Math.max(0.0, Math.max(boxMin - plane, plane - boxMax));
        return (float)(1.0 - smoothstep(1.25, REVEAL_DISTANCE, gap));
    }

    public static float endAlpha(double along, double center) {
        return (float)(1.0 - smoothstep(HALF_LENGTH - END_FADE, HALF_LENGTH, Math.abs(along - center)));
    }

    private static double smoothstep(double low, double high, double value) {
        double t = Math.max(0, Math.min(1, (value - low) / (high - low)));
        return t * t * (3 - 2 * t);
    }
}
