package dev.stya.blockzone.data.persistence;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.ptcrys.fpsmatch.core.capability.CapabilityMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BattlezoneTeamConfigurationTest {
    @Test void customAndSpectatorCapabilityDataRoundTrips() {
        var entries = List.of(new BattlezoneTeamConfiguration.Entry("squad_1", 3, new CapabilityMap.Wrapper(
                Map.of("StartKitsCapability", JsonParser.parseString("[]")))),
                new BattlezoneTeamConfiguration.Entry("spectator", 100, new CapabilityMap.Wrapper(Map.of())));
        var codec = BattlezoneTeamConfiguration.Entry.CODEC.listOf();
        var json = codec.encodeStart(JsonOps.INSTANCE, entries).result().orElseThrow();
        assertEquals(entries, codec.parse(JsonOps.INSTANCE, json).result().orElseThrow());
        assertFalse(json.toString().contains("players"));
    }
    @Test void duplicateNamesAreRejectedBeforeRestoration() {
        var entry = new BattlezoneTeamConfiguration.Entry("squad_1", 3, new CapabilityMap.Wrapper(Map.of()));
        assertThrows(IllegalArgumentException.class, () -> BattlezoneTeamConfiguration.validate(List.of(entry, entry)));
    }
    @Test void invalidTeamDefinitionsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new BattlezoneTeamConfiguration.Entry("", 3, new CapabilityMap.Wrapper(Map.of())));
        assertThrows(IllegalArgumentException.class, () -> new BattlezoneTeamConfiguration.Entry("squad_1", 0, new CapabilityMap.Wrapper(Map.of())));
    }
}
