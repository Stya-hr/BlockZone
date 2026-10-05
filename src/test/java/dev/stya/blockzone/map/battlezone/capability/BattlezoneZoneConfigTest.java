package dev.stya.blockzone.map.battlezone.capability;

import com.mojang.serialization.JsonOps;
import dev.stya.blockzone.zone.PoisonPathDefaults;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BattlezoneZoneConfigTest {
    @Test void zoneConfigurationRoundTripsAllFields() {
        var config = new BattlezoneZoneCapability.Config(List.of(PoisonPathDefaults.create(10, 20, 50)), 2.5, "blockzone:test.png");
        var json = BattlezoneZoneCapability.Config.CODEC.encodeStart(JsonOps.INSTANCE, config).result().orElseThrow();
        assertEquals(config, BattlezoneZoneCapability.Config.CODEC.parse(JsonOps.INSTANCE, json).result().orElseThrow());
    }
    @Test void editingPathsKeepsDamageAndTexture() {
        var config = new BattlezoneZoneCapability.Config(List.of(PoisonPathDefaults.create(0, 0, 80)), 3, "blockzone:test.png");
        var edited = config.withPaths(List.of(PoisonPathDefaults.create(20, 10, 40)));
        assertEquals(3, edited.poisonDamagePerSecond());
        assertEquals(config.boundaryTexture(), edited.boundaryTexture());
        assertEquals(80, config.poisonSequences().get(0).circles().get(0).radius());
    }
    @Test void invalidConfigurationIsRejected() {
        var paths = List.of(PoisonPathDefaults.create(0, 0, 80));
        assertThrows(IllegalArgumentException.class, () -> new BattlezoneZoneCapability.Config(List.of(), 1, "blockzone:test"));
        assertThrows(IllegalArgumentException.class, () -> new BattlezoneZoneCapability.Config(paths, Double.NaN, "blockzone:test"));
        assertThrows(IllegalArgumentException.class, () -> new BattlezoneZoneCapability.Config(paths, 1, "bad resource name"));
    }
}
