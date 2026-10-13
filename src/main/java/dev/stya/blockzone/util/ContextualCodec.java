package dev.stya.blockzone.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import java.util.function.Function;

/** Keep validation exceptions and decode failures inside DataResult, with their configuration location. */
public final class ContextualCodec {
    public static <T> Codec<T> wrap(Codec<T> codec, Function<Dynamic<?>, String> location) {
        return Codec.PASSTHROUGH.flatXmap(input -> {
            String prefix = location.apply(input);
            try { return codec.parse(input).mapError(message -> prefix + ": " + message); }
            catch (IllegalArgumentException failure) { return DataResult.error(() -> prefix + ": " + failure.getMessage()); }
        }, value -> codec.encodeStart(JsonOps.INSTANCE, value).map(json -> new Dynamic<>(JsonOps.INSTANCE, json)));
    }
    private ContextualCodec() { }
}
