package dev.stya.blockzone.map.battlezone;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** A horizontal route in world coordinates; speed is measured in blocks per second. */
public record FlightRoute(double startX, double startY, double startZ,
                                   double endX, double endY, double endZ, double speed) {
    public static final Codec<FlightRoute> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.DOUBLE.fieldOf("start_x").forGetter(FlightRoute::startX),
            Codec.DOUBLE.fieldOf("start_y").forGetter(FlightRoute::startY),
            Codec.DOUBLE.fieldOf("start_z").forGetter(FlightRoute::startZ),
            Codec.DOUBLE.fieldOf("end_x").forGetter(FlightRoute::endX),
            Codec.DOUBLE.fieldOf("end_y").forGetter(FlightRoute::endY),
            Codec.DOUBLE.fieldOf("end_z").forGetter(FlightRoute::endZ),
            Codec.doubleRange(0.1, 100.0).fieldOf("speed").forGetter(FlightRoute::speed)
    ).apply(instance, FlightRoute::new));

    public double length() {
        return Math.hypot(endX - startX, endZ - startZ);
    }

    public boolean isValid(double minX, double maxX, double minZ, double maxZ, double minimumHeight) {
        return Double.isFinite(startX) && Double.isFinite(startY) && Double.isFinite(startZ)
                && Double.isFinite(endX) && Double.isFinite(endY) && Double.isFinite(endZ)
                && Double.isFinite(speed) && speed >= 0.1 && speed <= 100
                && startY == endY && startY >= minimumHeight && startY < 2000
                && length() > 0.001
                && startX >= minX && startX <= maxX && endX >= minX && endX <= maxX
                && startZ >= minZ && startZ <= maxZ && endZ >= minZ && endZ <= maxZ;
    }

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
