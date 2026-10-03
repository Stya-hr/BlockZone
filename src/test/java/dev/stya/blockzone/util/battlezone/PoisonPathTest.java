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

    @Test void impossibleOrGrowingPathsAreRejectedWithoutAlteringConfig() {
        assertThrows(IllegalArgumentException.class, () -> new PoisonPath(List.of()).resolve(bounds, 64));
        assertThrows(IllegalArgumentException.class, () -> new PoisonPath(List.of(
                new PoisonPath.Circle(0, 0, 101, 0, 0))).resolve(bounds, 64));
        assertThrows(IllegalArgumentException.class, () -> new PoisonPath(List.of(
                new PoisonPath.Circle(0, 0, 20, 0, 0), new PoisonPath.Circle(0, 0, 30, 1, 1))).resolve(bounds, 64));
    }
}
