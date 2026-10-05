package dev.stya.blockzone.deployment;

import dev.stya.blockzone.zone.ZoneGeometry;
import java.util.Optional;

/** A horizontal route in world coordinates; speed is measured in blocks per second. */
public record FlightRoute(double startX, double startY, double startZ,
                                   double endX, double endY, double endZ, double speed) {
    /** A near-central chord: offsets up to 3% of radius keep area difference below 8% of the smaller area. */
    public static Optional<FlightRoute> generateAcrossCircle(ZoneGeometry zone, double minX, double maxX,
            double minZ, double maxZ, double altitude,
            double speed, double angle, double offsetFraction) {
        if (!(zone.radius() > 0) || !Double.isFinite(altitude)
                || !Double.isFinite(speed) || speed < .1 || speed > 100) {
            return Optional.empty();
        }
        double crossRadius = zone.radius() * .95;
        double dx = Math.cos(angle), dz = Math.sin(angle);
        double offset = zone.radius() * Math.max(-.03, Math.min(.03, offsetFraction));
        double cx = zone.centerX() - dz * offset, cz = zone.centerZ() + dx * offset;
        double halfLength = Math.sqrt(crossRadius * crossRadius - offset * offset);
        if (cx < minX || cx > maxX || cz < minZ || cz > maxZ) return Optional.empty();
        if (Math.abs(dx) > 1e-9) {
            halfLength = Math.min(halfLength, Math.min(cx - minX, maxX - cx) / Math.abs(dx));
        }
        if (Math.abs(dz) > 1e-9) {
            halfLength = Math.min(halfLength, Math.min(cz - minZ, maxZ - cz) / Math.abs(dz));
        }
        halfLength *= .999;
        var route = new FlightRoute(cx - dx * halfLength, altitude, cz - dz * halfLength,
                cx + dx * halfLength, altitude, cz + dz * halfLength, speed);
        return route.isValid(minX, maxX, minZ, maxZ)
                && zone.containsHorizontal(route.startX(), route.startZ())
                && zone.containsHorizontal(route.endX(), route.endZ()) ? Optional.of(route) : Optional.empty();
    }

    public double length() {
        return Math.hypot(endX - startX, endZ - startZ);
    }

    public boolean isValid(double minX, double maxX, double minZ, double maxZ) {
        return Double.isFinite(startX) && Double.isFinite(startY) && Double.isFinite(startZ)
                && Double.isFinite(endX) && Double.isFinite(endY) && Double.isFinite(endZ)
                && Double.isFinite(speed) && speed >= 0.1 && speed <= 100
                && startY == endY
                && length() > 0.001
                && startX >= minX && startX <= maxX && endX >= minX && endX <= maxX
                && startZ >= minZ && startZ <= maxZ && endZ >= minZ && endZ <= maxZ;
    }

    /** The transport keeps flying after the last automatic jump, until deployment ends. */
    public VehiclePosition vehiclePosition(double ticks) {
        double distance = Math.max(0, ticks) * speed / 20.0;
        double fraction = length() > .001 ? distance / length() : 0;
        return new VehiclePosition(startX + (endX - startX) * fraction,
                startZ + (endZ - startZ) * fraction);
    }

    public record VehiclePosition(double x, double z) { }

    public double progress(long ticks) {
        if (length() <= 0.001) {
            return 1.0;
        }
        return Math.min(1.0, Math.max(0.0, ticks * speed / 20.0 / length()));
    }

    public double x(long ticks) {
        return startX + (endX - startX) * progress(ticks);
    }

    public double z(long ticks) {
        return startZ + (endZ - startZ) * progress(ticks);
    }
}
