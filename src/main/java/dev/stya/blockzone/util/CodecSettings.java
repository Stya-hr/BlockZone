package dev.stya.blockzone.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.DataResult;
import com.ptcrys.fpsmatch.core.data.Setting;

/** Share the file codec with FPSMatch's command parser, including range validation. */
public final class CodecSettings {
    private CodecSettings() {}

    public static final Codec<Double> FINITE_DOUBLE = Codec.DOUBLE.flatXmap(CodecSettings::finite, CodecSettings::finite);

    public static final Codec<Double> NONNEGATIVE_DOUBLE = FINITE_DOUBLE.flatXmap(CodecSettings::nonnegative, CodecSettings::nonnegative);

    private static DataResult<Double> nonnegative(double value) {
        return value >= 0 && value <= Float.MAX_VALUE ? DataResult.success(value)
                : DataResult.error(() -> "Number must be nonnegative and fit a damage value");
    }

    private static DataResult<Double> finite(double value) {
        return Double.isFinite(value) ? DataResult.success(value) : DataResult.error(() -> "Number must be finite");
    }

    /** DFU's optionalFieldOf ignores malformed values in this Minecraft version; only absence may default. */
    public static <T> com.mojang.serialization.MapCodec<T> optionalField(Codec<T> codec, String name, T fallback) {
        return new com.mojang.serialization.MapCodec<>() {
            @Override public <O> DataResult<T> decode(com.mojang.serialization.DynamicOps<O> ops, com.mojang.serialization.MapLike<O> input) {
                O value = input.get(name);
                return value == null ? DataResult.success(fallback) : codec.parse(ops, value);
            }
            @Override public <O> com.mojang.serialization.RecordBuilder<O> encode(T input,
                    com.mojang.serialization.DynamicOps<O> ops, com.mojang.serialization.RecordBuilder<O> prefix) {
                return prefix.add(name, codec.encodeStart(ops, input));
            }
            @Override public <O> java.util.stream.Stream<O> keys(com.mojang.serialization.DynamicOps<O> ops) {
                return java.util.stream.Stream.of(ops.createString(name));
            }
        };
    }

    /** Marker used by the FPSMatch room settings adapter for codec-backed JSON fields. */
    public static final class JsonSetting<T> extends Setting<T> {
        private JsonSetting(String category, String name, Codec<T> codec, T initial) {
            super(category, name, codec, initial, text -> CodecSettings.parse(codec, text));
        }
        @Override public String toString() { return toJson().toString(); }
    }

    public static <T> com.ptcrys.fpsmatch.common.packet.mapselect.MapRoomSettingInfo forUi(Setting<T> setting,
            com.ptcrys.fpsmatch.common.packet.mapselect.MapRoomSettingInfo previous) {
        if (!(setting instanceof JsonSetting<?>) || !setting.toJson().isJsonArray()) return previous;
        String initial = setting.codec().encodeStart(JsonOps.INSTANCE, setting.getDefaultValue())
                .getOrThrow(false, message -> {}).toString();
        return new com.ptcrys.fpsmatch.common.packet.mapselect.MapRoomSettingInfo(previous.name(), setting.toJson().toString(), initial,
                previous.editable(), previous.translationKey(), com.ptcrys.fpsmatch.common.packet.mapselect.MapRoomSettingInfo.SettingType.STRING,
                previous.descriptionKey(), false, 0, 0, 1, previous.category());
    }

    public static <T> Setting<T> create(String category, String name, Codec<T> codec, T initial) {
        return new JsonSetting<>(category, name, codec, initial);
    }

    static <T> T parse(Codec<T> codec, String text) {
        JsonElement json = JsonParser.parseString(text);
        requireFiniteNumbers(json);
        return codec.parse(JsonOps.INSTANCE, json).getOrThrow(false, message -> {});
    }

    private static void requireFiniteNumbers(JsonElement json) {
        if (json.isJsonArray()) json.getAsJsonArray().forEach(CodecSettings::requireFiniteNumbers);
        else if (json.isJsonObject()) json.getAsJsonObject().entrySet()
                .forEach(entry -> requireFiniteNumbers(entry.getValue()));
        else if (json.isJsonPrimitive() && json.getAsJsonPrimitive().isNumber()
                && !Double.isFinite(json.getAsDouble())) {
            throw new IllegalArgumentException("Setting numbers must be finite");
        }
    }
}
