package dev.stya.blockzone.util.battlezone;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class PoisonPathTest {
    private final BoundaryGeometry bounds = BoundaryGeometry.of(-100, -100, 99, 99);

    @Test void independentPathsRoundTripWithTheirOwnTimingAndInitialCircle() {
        String json = """
                [{"circles":[{"x":10,"z":20,"radius":80,"wait_seconds":0,"shrink_seconds":0},
                {"x":30,"z":40,"radius":20,"wait_seconds":12,"shrink_seconds":25}]},
                {"circles":[{"x":-10,"z":-20,"radius":60,"wait_seconds":0,"shrink_seconds":0},
                {"x":-30,"z":-40,"radius":0,"wait_seconds":5,"shrink_seconds":15}]}]
                """;
        var codec = PoisonPath.CODEC.listOf();
        var paths = codec.parse(JsonOps.INSTANCE, JsonParser.parseString(json)).result().orElseThrow();
        assertEquals(2, paths.size());
        assertEquals(80, paths.get(0).resolve(bounds, 64).get(0).radius());
        assertEquals(60, paths.get(1).resolve(bounds, 64).get(0).radius());
        assertEquals(12, paths.get(0).circles().get(1).waitSeconds());
        assertEquals(15, paths.get(1).circles().get(1).shrinkSeconds());
        var encoded = codec.encodeStart(JsonOps.INSTANCE, paths).result().orElseThrow();
        assertEquals(paths, codec.parse(JsonOps.INSTANCE, encoded).result().orElseThrow());
    }

    @Test void resolvingDoesNotModifyConfiguredCentersOrRadii() {
        var path = new PoisonPath(List.of(new PoisonPath.Circle(500, -500, 80, 0, 0),
                new PoisonPath.Circle(-500, 500, 20, 10, 20)));
        var actual = path.resolve(bounds, 64);
        assertEquals(new ZoneGeometry(20, 64, -20, 80), actual.get(0));
        assertEquals(new ZoneGeometry(-80, 64, 80, 20), actual.get(1));
        assertEquals(500, path.circles().get(0).x());
        assertEquals(20, path.circles().get(1).radius());
    }

    @Test void impossiblePathsAreRejectedWithoutAlteringConfig() {
        assertThrows(IllegalArgumentException.class, () -> new PoisonPath(List.of()).resolve(bounds, 64));
        assertThrows(IllegalArgumentException.class, () -> new PoisonPath(List.of(
                new PoisonPath.Circle(0, 0, 101, 0, 0))).resolve(bounds, 64));
        var expanding = new PoisonPath(List.of(new PoisonPath.Circle(0, 0, 20, 0, 0),
                new PoisonPath.Circle(0, 0, 30, 1, 1))).resolve(bounds, 64);
        assertEquals(20, expanding.get(0).radius());
        assertEquals(30, expanding.get(1).radius());
    }
    @Test void damageMultipliersDefaultForOldFilesAndApplyAfterEachCompletedTransition() {
        var path = new PoisonPath(List.of(new PoisonPath.Circle(0, 0, 80, 0, 0, .5),
                new PoisonPath.Circle(0, 0, 40, 5, 10, 2), new PoisonPath.Circle(0, 0, 0, 5, 10, 4)));
        assertEquals(1, path.damageAt(0, 2));
        assertEquals(4, path.damageAt(1, 2));
        assertEquals(8, path.damageAt(2, 2));
        assertEquals(8, path.damageAt(3, 2));
        assertEquals(0, path.damageAt(1, 0));
        var json = PoisonPath.CODEC.encodeStart(JsonOps.INSTANCE, path).result().orElseThrow();
        assertEquals(path, PoisonPath.CODEC.parse(JsonOps.INSTANCE, json).result().orElseThrow());
        var old = JsonParser.parseString("{\"x\":0,\"z\":0,\"radius\":10,\"wait_seconds\":0,\"shrink_seconds\":0}");
        var circle = PoisonPath.CIRCLE_CODEC.parse(JsonOps.INSTANCE, old).result().orElseThrow();
        assertEquals(1, circle.damageMultiplier());
        assertTrue(PoisonPath.CIRCLE_CODEC.encodeStart(JsonOps.INSTANCE, circle).result().orElseThrow().getAsJsonObject().has("damageMultiplier"));
        for (String invalid : List.of("-1", "1e999", "\"NaN\"")) {
            var input = old.deepCopy().getAsJsonObject();
            input.add("damage_multiplier", JsonParser.parseString(invalid));
            assertTrue(PoisonPath.CIRCLE_CODEC.parse(JsonOps.INSTANCE, input).error().isPresent());
        }
    }
    @Test void legacyJsonReadsButOnlyCamelCaseIsWrittenAndCanonicalInvalidValuesAreNotIgnored() {
        var old = JsonParser.parseString("""
                {"x":1,"z":2,"radius":3,"wait_seconds":4,"shrink_seconds":5,"damage_multiplier":6}
                """).getAsJsonObject();
        var decoded = PoisonPath.CIRCLE_CODEC.parse(JsonOps.INSTANCE, old).result().orElseThrow();
        var encoded = PoisonPath.CIRCLE_CODEC.encodeStart(JsonOps.INSTANCE, decoded).result().orElseThrow().getAsJsonObject();
        assertEquals(4, encoded.get("waitSeconds").getAsInt());
        assertEquals(5, encoded.get("shrinkSeconds").getAsInt());
        assertEquals(6, encoded.get("damageMultiplier").getAsDouble());
        assertFalse(encoded.has("wait_seconds")); assertFalse(encoded.has("shrink_seconds")); assertFalse(encoded.has("damage_multiplier"));
        old.addProperty("waitSeconds", 10); old.addProperty("damageMultiplier", 2);
        decoded = PoisonPath.CIRCLE_CODEC.parse(JsonOps.INSTANCE, old).result().orElseThrow();
        assertEquals(10, decoded.waitSeconds()); assertEquals(2, decoded.damageMultiplier());
        old.addProperty("damageMultiplier", -1);
        assertTrue(PoisonPath.CIRCLE_CODEC.parse(JsonOps.INSTANCE, old).error().isPresent());
        old.addProperty("damageMultiplier", 2); old.addProperty("waitSeconds", -1);
        assertTrue(PoisonPath.CIRCLE_CODEC.parse(JsonOps.INSTANCE, old).error().isPresent());
    }
}
