package dev.stya.blockzone.zone;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.Locale;

/** Shape is fixed for an entire shrink sequence. */
public enum ZoneShape {
    CYLINDER, SQUARE_PRISM;

    public static final Codec<ZoneShape> CODEC = Codec.STRING.comapFlatMap(value -> {
        for (var shape : values()) if (shape.id().equals(value)) return DataResult.success(shape);
        return DataResult.error(() -> "Unknown poison shape: " + value + "; expected cylinder or square_prism");
    }, ZoneShape::id);

    public String id() { return name().toLowerCase(Locale.ROOT); }
}
