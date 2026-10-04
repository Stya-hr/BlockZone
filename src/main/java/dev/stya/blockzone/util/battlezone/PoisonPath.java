package dev.stya.blockzone.util.battlezone;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;

/** A complete configured path: first circle is deployment, subsequent circles are shrink targets. */
public record PoisonPath(List<Circle> circles, ZoneShape shape) {
    public PoisonPath(List<Circle> circles) { this(circles, ZoneShape.CYLINDER); }
    public PoisonPath { circles = List.copyOf(circles); }
    public static final Codec<Circle> CIRCLE_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            dev.stya.blockzone.util.CodecSettings.FINITE_DOUBLE.fieldOf("x").forGetter(Circle::x),
            dev.stya.blockzone.util.CodecSettings.FINITE_DOUBLE.fieldOf("z").forGetter(Circle::z),
            Codec.doubleRange(0, 30_000_000).fieldOf("radius").forGetter(Circle::radius),
            dev.stya.blockzone.util.CodecSettings.aliasedField(Codec.intRange(0, 1_000_000), "waitSeconds", "wait_seconds", null).forGetter(Circle::waitSeconds),
            dev.stya.blockzone.util.CodecSettings.aliasedField(Codec.intRange(0, 1_000_000), "shrinkSeconds", "shrink_seconds", null).forGetter(Circle::shrinkSeconds),
            dev.stya.blockzone.util.CodecSettings.aliasedField(dev.stya.blockzone.util.CodecSettings.NONNEGATIVE_DOUBLE, "damageMultiplier", "damage_multiplier", 1.0).forGetter(Circle::damageMultiplier)
    ).apply(instance, Circle::new));
    public static final Codec<PoisonPath> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            CIRCLE_CODEC.listOf().fieldOf("circles").forGetter(PoisonPath::circles),
            dev.stya.blockzone.util.CodecSettings.optionalField(ZoneShape.CODEC, "shape", ZoneShape.CYLINDER).forGetter(PoisonPath::shape)
    ).apply(instance, PoisonPath::new));

    public List<ZoneGeometry> resolve(BoundaryGeometry bounds, double y) {
        if (circles.isEmpty()) throw new IllegalArgumentException("Path must contain an initial circle");
        var result = new ArrayList<ZoneGeometry>();
        for (var circle : circles) {
            if (!Double.isFinite(circle.x()) || !Double.isFinite(circle.z()) || !Double.isFinite(circle.radius())
                    || !Double.isFinite(circle.damageMultiplier()) || circle.damageMultiplier() < 0)
                throw new IllegalArgumentException("Circle numbers must be finite and damageMultiplier nonnegative");
            if (circle.waitSeconds() < 0 || circle.shrinkSeconds() < 0) throw new IllegalArgumentException("Negative circle duration");
            result.add(new ZoneGeometry(circle.x(), y, circle.z(), circle.radius(), shape).fitInside(bounds));
        }
        return List.copyOf(result);
    }
    public double damageAt(int completedTransitions, double baseDamage) {
        return Math.min(Float.MAX_VALUE, baseDamage * circles.get(Math.min(completedTransitions, circles.size() - 1)).damageMultiplier());
    }
    public record Circle(double x, double z, double radius, int waitSeconds, int shrinkSeconds, double damageMultiplier) {
        public Circle(double x, double z, double radius, int waitSeconds, int shrinkSeconds) {
            this(x, z, radius, waitSeconds, shrinkSeconds, 1.0);
        }
    }
}
