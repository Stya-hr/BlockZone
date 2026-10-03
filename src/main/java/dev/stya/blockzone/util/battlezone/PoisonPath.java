package dev.stya.blockzone.util.battlezone;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;

/** A complete configured path: first circle is deployment, subsequent circles are shrink targets. */
public record PoisonPath(List<Circle> circles) {
    public PoisonPath { circles = List.copyOf(circles); }
    public static final Codec<Circle> CIRCLE_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.DOUBLE.fieldOf("x").forGetter(Circle::x),
            Codec.DOUBLE.fieldOf("z").forGetter(Circle::z),
            Codec.doubleRange(0, 30_000_000).fieldOf("radius").forGetter(Circle::radius),
            Codec.intRange(0, 1_000_000).fieldOf("wait_seconds").forGetter(Circle::waitSeconds),
            Codec.intRange(0, 1_000_000).fieldOf("shrink_seconds").forGetter(Circle::shrinkSeconds)
    ).apply(instance, Circle::new));
    public static final Codec<PoisonPath> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            CIRCLE_CODEC.listOf().fieldOf("circles").forGetter(PoisonPath::circles)
    ).apply(instance, PoisonPath::new));

    public List<ZoneGeometry> resolve(BoundaryGeometry bounds, double y) {
        if (circles.isEmpty()) throw new IllegalArgumentException("Path must contain an initial circle");
        var result = new ArrayList<ZoneGeometry>();
        double previous = Double.POSITIVE_INFINITY;
        for (var circle : circles) {
            if (circle.radius() > previous) throw new IllegalArgumentException("Circle radii must not increase");
            if (circle.waitSeconds() < 0 || circle.shrinkSeconds() < 0) throw new IllegalArgumentException("Negative circle duration");
            result.add(new ZoneGeometry(circle.x(), y, circle.z(), circle.radius()).fitInside(bounds));
            previous = circle.radius();
        }
        return List.copyOf(result);
    }
    public record Circle(double x, double z, double radius, int waitSeconds, int shrinkSeconds) {}
}
