package dev.stya.blockzone.inventory;

import com.google.gson.JsonParser;
import dev.stya.blockzone.map.battlezone.capability.BattlezoneInventoryCapability;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class InventoryRulesTest {
    @Test void allPanelSlotsHaveStableUniqueIdsAndIndices() {
        assertEquals(17, InventorySlot.ALL.size());
        assertEquals(17, InventoryRules.defaults().slots().size());
        for (var slot : InventorySlot.ALL) {
            assertSame(slot, InventorySlot.byIndex(slot.index()));
            assertSame(slot, InventorySlot.byId(slot.id()));
        }
        assertThrows(IllegalArgumentException.class, () -> InventorySlot.byId("weapon.typo"));
        assertThrows(IllegalArgumentException.class, () -> InventorySlot.byIndex(17));
    }

    @Test void capabilityRoundTripsAndSingleSlotUpdatePreservesOtherRules() {
        var cap = new BattlezoneInventoryCapability(null);
        var changed = cap.read().withSlot(InventorySlot.PRIMARY_WEAPON,
                new InventoryRules.Definition(List.of("@gun:tacz:ak47"), 1));
        cap.write(changed);
        var restored = new BattlezoneInventoryCapability(null);
        restored.decode(cap.toJson());
        assertEquals(changed, restored.read());
        assertEquals(InventoryRules.defaults().slots().get("equipment.helmet"), changed.slots().get("equipment.helmet"));
        assertThrows(UnsupportedOperationException.class, () -> changed.slots().clear());
        assertThrows(UnsupportedOperationException.class, () -> changed.slots().get("weapon.primary").allowed().clear());
    }

    @Test void malformedUpdatesAreAtomicAndCannotIntroduceUnknownSlots() {
        var cap = new BattlezoneInventoryCapability(null);
        var previous = cap.read();
        for (String json : List.of(
                "{\"weapon.primary\":{\"allowed\":[\"minecraft:stone\"],\"limit\":0}}",
                "{\"weapon.typo\":{\"allowed\":[],\"limit\":1}}",
                "{\"weapon.primary\":{\"allowed\":[\"bad id\"],\"limit\":1}}")) {
            assertThrows(RuntimeException.class, () -> cap.decode(JsonParser.parseString(json)));
            assertEquals(previous, cap.read());
        }
    }

    @Test void mapsFreezeIndependentMatchRulesUntilReset() {
        var first = new BattlezoneInventoryCapability(null);
        var second = new BattlezoneInventoryCapability(null);
        first.begin();
        var active = first.rule(InventorySlot.HELMET);
        first.write(new InventoryRules(Map.of("equipment.helmet", new InventoryRules.Definition(List.of("*"), 3))));
        assertSame(active, first.rule(InventorySlot.HELMET));
        assertEquals(1, second.rule(InventorySlot.HELMET).limit());
        first.reset();
        assertEquals(3, first.rule(InventorySlot.HELMET).limit());
        assertEquals(first.read(), new InventoryRules(first.read().slots()));
    }
}
