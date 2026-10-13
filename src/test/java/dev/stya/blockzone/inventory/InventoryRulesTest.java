package dev.stya.blockzone.inventory;

import com.google.gson.JsonParser;
import dev.stya.blockzone.map.battlezone.capability.BattlezoneInventoryCapability;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class InventoryRulesTest {
    @Test void typesAreFixedAndOmittedTypesAreInferred() {
        var rules = new InventoryRules(Map.of("accessory.1", new InventoryRules.Definition(List.of("*"), 1)));
        assertEquals("accessory", rules.slots().get("accessory.1").type());
        assertThrows(IllegalArgumentException.class, () -> new InventoryRules(Map.of("accessory.1",
                new InventoryRules.Definition(List.of("*"), 1, Map.of(), "weapon"))));
        assertThrows(IllegalArgumentException.class, () -> rules.withSlot(InventorySlot.ACCESSORY_1,
                new InventoryRules.Definition(List.of("*"), 2)));
        assertThrows(IllegalArgumentException.class, () -> rules.withSlot(InventorySlot.BOOTS,
                new InventoryRules.Definition(List.of("*"), 1, Map.of("*", 2))));
    }
    @Test void itemLimitsRoundTripAndOmittedLimitsUseDefaults() {
        var cap = new BattlezoneInventoryCapability(null);
        var definition = new InventoryRules.Definition(List.of("minecraft:golden_apple", "minecraft:paper"), 4,
                Map.of("minecraft:golden_apple", 8, "minecraft:paper", 16));
        cap.write(cap.read().withSlot(InventorySlot.MEDKIT, definition));
        var restored = new BattlezoneInventoryCapability(null);
        restored.decode(cap.toJson());
        assertEquals(cap.read(), restored.read());
        assertThrows(UnsupportedOperationException.class, () -> definition.itemLimits().clear());
        restored.decode(JsonParser.parseString("{\"item.medkit\":{\"allowed\":[\"minecraft:paper\"],\"limit\":4}}"));
        assertTrue(restored.read().slots().get("item.medkit").itemLimits().isEmpty());
        assertEquals(4, restored.rule(InventorySlot.MEDKIT).limit());
    }

    @Test void invalidItemLimitsCannotChangeCapability() {
        var cap = new BattlezoneInventoryCapability(null);
        var before = cap.read();
        for (String overrides : List.of("{\"minecraft:paper\":0}", "{\"minecraft:paper\":1025}",
                "{\"minecraft:stone\":8}", "{\"minecraft:paper\":\"bad\"}", "[]", "null")) {
            String json = "{\"item.medkit\":{\"allowed\":[\"minecraft:paper\"],\"limit\":4,\"item_limits\":" + overrides + "}}";
            assertThrows(RuntimeException.class, () -> cap.decode(JsonParser.parseString(json)), overrides);
            assertEquals(before, cap.read());
        }
    }

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

    @Test void diagnosticsIdentifySlotAndInvalidFieldWithoutChangingConfiguration() {
        var cap = new BattlezoneInventoryCapability(null);
        var before = cap.read();
        for (String rule : List.of("{\"allowed\":[\"minecraft:paper\"],\"limit\":0}",
                "{\"allowed\":[\"bad id\"],\"limit\":4}",
                "{\"allowed\":[\"minecraft:paper\"],\"limit\":4,\"item_limits\":{\"minecraft:paper\":0}}")) {
            var json = JsonParser.parseString("{\"item.medkit\":" + rule + "}");
            var result = InventoryRules.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, json);
            assertTrue(result.error().orElseThrow().message().contains("item.medkit"));
            var failure = assertThrows(RuntimeException.class, () -> cap.decode(json));
            assertTrue(failure.getMessage().contains("item.medkit"), failure.getMessage());
            assertEquals(before, cap.read());
        }
    }

    @Test void mapsFreezeIndependentMatchRulesUntilReset() {
        var first = new BattlezoneInventoryCapability(null);
        var second = new BattlezoneInventoryCapability(null);
        first.begin();
        var active = first.rule(InventorySlot.MEDKIT);
        first.write(new InventoryRules(Map.of("item.medkit", new InventoryRules.Definition(List.of("*"), 3))));
        assertSame(active, first.rule(InventorySlot.MEDKIT));
        assertEquals(64, second.rule(InventorySlot.MEDKIT).limit());
        first.reset();
        assertEquals(3, first.rule(InventorySlot.MEDKIT).limit());
        assertEquals(first.read(), new InventoryRules(first.read().slots()));
    }
}
