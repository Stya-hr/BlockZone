package dev.stya.blockzone.map.battlezone.capability;

import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BattlezoneLoadoutCapabilityTest {
    @Test void emptyLoadoutSurvivesSerializationAndMatchReset() {
        var capability = new BattlezoneLoadoutCapability(null);
        capability.write(List.of());
        capability.begin(List.of());
        capability.reset();
        var restored = new BattlezoneLoadoutCapability(null);
        restored.decode(capability.toJson());
        assertEquals(List.of(), restored.read());
    }

    @Test void callerCannotChangeSavedConfiguration() {
        var capability = new BattlezoneLoadoutCapability(null);
        var entries = new ArrayList<dev.stya.blockzone.equipment.StartingLoadout.Entry>();
        capability.write(entries);
        entries.addAll(new BattlezoneLoadoutCapability(null).read());
        assertTrue(capability.read().isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> capability.read().add(entries.get(0)));
    }

    @Test void malformedSavedLoadoutDoesNotReplaceConfiguration() {
        var capability = new BattlezoneLoadoutCapability(null);
        var before = capability.read();
        assertThrows(RuntimeException.class, () -> capability.decode(JsonParser.parseString(
                "[{\"slot\":\"head\",\"item\":\"blockzone:tactical_helmet\",\"count\":0}]")));
        assertEquals(before, capability.read());
    }
}
