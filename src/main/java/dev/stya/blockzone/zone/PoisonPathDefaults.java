package dev.stya.blockzone.zone;

import java.util.ArrayList;

/** Default path generation; configuration is never migrated from legacy settings. */
public final class PoisonPathDefaults {
    private PoisonPathDefaults() { }
    public static PoisonPath create(double x, double z, double radius) {
        var circles = new ArrayList<PoisonPath.Circle>();
        circles.add(new PoisonPath.Circle(x, z, radius, 0, 0));
        for (double fraction : new double[]{.8, .55, .25, 0})
            circles.add(new PoisonPath.Circle(x, z, radius * fraction, 30, 60));
        return new PoisonPath(circles);
    }
}
