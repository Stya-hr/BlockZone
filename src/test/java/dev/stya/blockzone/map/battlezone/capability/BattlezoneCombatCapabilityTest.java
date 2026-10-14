package dev.stya.blockzone.map.battlezone.capability;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BattlezoneCombatCapabilityTest {
    @Test void configurationChangesApplyOnlyWhenNextMatchBegins() {
        var cap = new BattlezoneCombatCapability(null);
        cap.begin();
        cap.write(new BattlezoneCombatCapability.Config(200, 75, 70, 90, 4, 3, .5, .4, 2, .5, .1));
        assertEquals(100, cap.controller().health());
        assertEquals(50, cap.controller().platePoints());
        assertEquals(BattlezoneCombatCapability.defaults(), cap.controller().config());
        cap.reset();
        cap.begin();
        assertEquals(200, cap.controller().health());
        assertEquals(75, cap.controller().platePoints());
        assertEquals(cap.read(), cap.controller().config());
    }
    @Test void configurationSurvivesSerializationAndReset() {
        var cap = new BattlezoneCombatCapability(null);
        cap.write(new BattlezoneCombatCapability.Config(125, 60, 40, 45, 6, 4, .4, .2, 3, .1, .2));
        var restored = new BattlezoneCombatCapability(null);
        restored.decode(cap.toJson());
        restored.reset();
        assertEquals(cap.read(), restored.read());
    }
    @Test void invalidConfigurationLeavesExistingValuesIntact() {
        var cap = new BattlezoneCombatCapability(null);
        assertThrows(RuntimeException.class, () -> cap.decode(JsonParser.parseString("{\"matchHealth\":0,\"armorPlatePoints\":50}")));
        assertEquals(BattlezoneCombatCapability.defaults(), cap.read());
        assertThrows(IllegalArgumentException.class, () -> new BattlezoneCombatCapability.Config(Double.NaN, 50, 50, 60, 8, 2.5, .3, .25, 5, .25, .05));
    }
    @Test void oldPartialConfigurationIsRejected() {
        var cap = new BattlezoneCombatCapability(null);
        assertThrows(RuntimeException.class, () -> cap.decode(JsonParser.parseString(
                "{\"matchHealth\":100,\"armorPlatePoints\":50}")));
        assertEquals(BattlezoneCombatCapability.defaults(), cap.read());
    }
    @Test void everyFieldEnforcesItsRangeWithoutChangingConfiguration() {
        var invalid = java.util.Map.ofEntries(
                java.util.Map.entry("matchHealth", 0.0), java.util.Map.entry("armorPlatePoints", 1025.0),
                java.util.Map.entry("downedHealth", 0.0), java.util.Map.entry("bleedSeconds", 0.0),
                java.util.Map.entry("rescueSeconds", 3601.0), java.util.Map.entry("rescueDistance", 0.0),
                java.util.Map.entry("reviveHealthFraction", 0.0), java.util.Map.entry("downedSpeedFraction", 1.1),
                java.util.Map.entry("recoveryDelaySeconds", -1.0), java.util.Map.entry("recoveryIntervalSeconds", 0.0),
                java.util.Map.entry("recoveryHealthFraction", 1.1));
        for (var entry : invalid.entrySet()) {
            var cap = new BattlezoneCombatCapability(null);
            var json = cap.toJson().getAsJsonObject();
            json.addProperty(entry.getKey(), entry.getValue());
            assertThrows(RuntimeException.class, () -> cap.decode(json), entry.getKey());
            assertEquals(BattlezoneCombatCapability.defaults(), cap.read(), entry.getKey());
        }
        assertEquals(0, BattlezoneCombatCapability.Config.ticks(0));
        assertEquals(1, BattlezoneCombatCapability.Config.ticks(.05));
        assertEquals(2, BattlezoneCombatCapability.Config.ticks(.051));
    }
}
