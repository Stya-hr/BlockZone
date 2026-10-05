package dev.stya.blockzone.map.battlezone.capability;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BattlezoneCombatCapabilityTest {
    @Test void configurationChangesApplyOnlyWhenNextMatchBegins() {
        var cap = new BattlezoneCombatCapability(null);
        cap.begin();
        cap.write(new BattlezoneCombatCapability.Config(200, 75));
        assertEquals(100, cap.controller().health());
        assertEquals(50, cap.controller().platePoints());
        cap.reset();
        cap.begin();
        assertEquals(200, cap.controller().health());
        assertEquals(75, cap.controller().platePoints());
    }
    @Test void configurationSurvivesSerializationAndReset() {
        var cap = new BattlezoneCombatCapability(null);
        cap.write(new BattlezoneCombatCapability.Config(125, 60));
        var restored = new BattlezoneCombatCapability(null);
        restored.decode(cap.toJson());
        restored.reset();
        assertEquals(cap.read(), restored.read());
    }
    @Test void invalidConfigurationLeavesExistingValuesIntact() {
        var cap = new BattlezoneCombatCapability(null);
        assertThrows(RuntimeException.class, () -> cap.decode(JsonParser.parseString("{\"matchHealth\":0,\"armorPlatePoints\":50}")));
        assertEquals(BattlezoneCombatCapability.defaults(), cap.read());
        assertThrows(IllegalArgumentException.class, () -> new BattlezoneCombatCapability.Config(Double.NaN, 50));
    }
}
