package dev.stya.blockzone.util.battlezone;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.mojang.serialization.JsonOps;
import java.util.ArrayList;
import java.util.List;

/** Convert legacy settings once when reading, leaving the source file intact until settings save. */
public final class PoisonSettingsMigration {
    private PoisonSettingsMigration() {}

    public static PoisonPath defaults(double x, double z, double radius) {
        var circles = new ArrayList<PoisonPath.Circle>();
        circles.add(new PoisonPath.Circle(x, z, radius, 0, 0));
        for (double fraction : new double[]{.8, .55, .25, 0})
            circles.add(new PoisonPath.Circle(x, z, radius * fraction, 30, 60));
        return new PoisonPath(circles);
    }

    public static JsonObject migrate(JsonElement config, double x, double z, double radius) {
        JsonObject result = config.getAsJsonObject().deepCopy();
        // Canonical names win when both spellings are present. Never mutate the caller's JSON.
        String[][] aliases = {
                {"team_player_limit","teamPlayerLimit"},
                {"total_player_limit","totalPlayerLimit"},
                {"minimum_teams_to_start","minimumTeamsToStart"},
                {"countdown_seconds","countdownSeconds"},
                {"deployment_seconds","deploymentSeconds"},
                {"settlement_seconds","settlementSeconds"},
                {"deployment_speed","deploymentSpeed"},
                {"deployment_height","deploymentHeight"},
                {"poison_damage_per_second","poisonDamagePerSecond"},
                {"poison_sequences","poisonSequences"},
                {"boundary_texture","boundaryTexture"}
        };
        for (String[] alias : aliases) {
            if (!result.has(alias[1]) && result.has(alias[0])) result.add(alias[1], result.get(alias[0]));
            result.remove(alias[0]);
        }
        JsonElement sequences = result.get("poisonSequences");
        if (sequences != null && (!sequences.isJsonArray() || !sequences.getAsJsonArray().isEmpty())) return result;
        JsonArray centers = result.has("poison_final_centers") ? result.getAsJsonArray("poison_final_centers") : new JsonArray();
        if (centers.isEmpty()) {
            var center = new JsonObject();
            center.addProperty("x", number(result, "poison_center_x", x));
            center.addProperty("z", number(result, "poison_center_z", z));
            centers.add(center);
        }
        var paths = new ArrayList<PoisonPath>();
        for (JsonElement value : centers) {
            var center = value.getAsJsonObject();
            double cx = number(center, "x", x), cz = number(center, "z", z);
            var circles = new ArrayList<PoisonPath.Circle>();
            circles.add(new PoisonPath.Circle(x, z, radius, 0, 0));
            if (result.has("poison_phases")) {
                for (JsonElement phaseValue : result.getAsJsonArray("poison_phases")) {
                    var phase = phaseValue.getAsJsonObject();
                    double targetRadius = radius * Math.max(0, Math.min(1, number(phase, "target_radius_fraction", 0)));
                    circles.add(new PoisonPath.Circle(cx, cz, targetRadius,
                            (int) number(phase, "wait_seconds", 30), (int) number(phase, "shrink_seconds", 60)));
                }
            } else circles.addAll(defaults(cx, cz, radius).circles().subList(1, 5));
            paths.add(new PoisonPath(circles));
        }
        result.add("poisonSequences", PoisonPath.CODEC.listOf().encodeStart(JsonOps.INSTANCE, paths)
                .getOrThrow(false, message -> {}));
        return result;
    }

    private static double number(JsonObject json, String key, double fallback) {
        return json.has(key) ? json.get(key).getAsDouble() : fallback;
    }
}
