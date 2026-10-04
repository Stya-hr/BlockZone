package dev.stya.blockzone.util.battlezone;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PoisonSettingsMigrationTest {
    @Test void legacyCentersBecomeIndependentPathsAndPreserveTimingWithoutMutatingSource() {
        var original = JsonParser.parseString("""
                {"poison_sequences":[],"poison_center_x":12,"poison_center_z":15,
                "poison_final_centers":[{"x":20,"z":30},{"x":-20,"z":-30}],
                "poison_phases":[{"wait_seconds":12,"shrink_seconds":24,"target_radius_fraction":0.5},
                {"wait_seconds":6,"shrink_seconds":9,"target_radius_fraction":0.8}],"poison_damage_per_second":2}
                """);
        var migrated = PoisonSettingsMigration.migrate(original, 0, 0, 80);
        var paths = PoisonPath.CODEC.listOf().parse(JsonOps.INSTANCE, migrated.get("poison_sequences")).result().orElseThrow();
        assertEquals(2, paths.size());
        assertEquals(new PoisonPath.Circle(0, 0, 80, 0, 0), paths.get(0).circles().get(0));
        assertEquals(new PoisonPath.Circle(20, 30, 40, 12, 24), paths.get(0).circles().get(1));
        assertEquals(64, paths.get(0).circles().get(2).radius());
        assertEquals(-20, paths.get(1).circles().get(1).x());
        assertEquals(2, migrated.get("poison_damage_per_second").getAsInt());
        assertTrue(original.getAsJsonObject().getAsJsonArray("poison_sequences").isEmpty());
    }

    @Test void completeSequencesTakePriorityOverLegacyValues() {
        var original = JsonParser.parseString("""
                {"poison_sequences":[{"circles":[{"x":1,"z":2,"radius":3,"wait_seconds":0,"shrink_seconds":0,"damage_multiplier":4}]}],
                "poison_center_x":99,"poison_phases":[]}
                """);
        assertEquals(original, PoisonSettingsMigration.migrate(original, 0, 0, 80));
    }
}
