package dev.stya.blockzone.inventory;

import com.google.gson.JsonParser;
import dev.stya.blockzone.map.battlezone.capability.BattlezoneInventoryCapability;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class InventoryRulesTest {
    @Test void typesAreFixedAndOmittedTypesAreInferred() {
        var rules = new InventoryRules(Map.of("accessory", new InventoryRules.Definition(List.of("*"), 1)));
        assertEquals("accessory", rules.slots().get("accessory").type());
        assertThrows(IllegalArgumentException.class, () -> new InventoryRules(Map.of("accessory",
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
        cap.write(cap.read().withSlot(InventorySlot.TACTICAL_2, definition));
        var restored = new BattlezoneInventoryCapability(null);
        restored.decode(cap.toJson());
        assertEquals(cap.read(), restored.read());
        assertThrows(UnsupportedOperationException.class, () -> definition.itemLimits().clear());
        restored.decode(JsonParser.parseString("{\"item.tactical\":{\"allowed\":[\"minecraft:paper\"],\"limit\":4}}"));
        assertTrue(restored.read().slots().get("item.tactical").itemLimits().isEmpty());
        assertEquals(4, restored.rule(InventorySlot.TACTICAL_2).limit());
    }

    @Test void invalidItemLimitsCannotChangeCapability() {
        var cap = new BattlezoneInventoryCapability(null);
        var before = cap.read();
        for (String overrides : List.of("{\"minecraft:paper\":0}", "{\"minecraft:paper\":1025}",
                "{\"minecraft:stone\":8}", "{\"minecraft:paper\":\"bad\"}", "[]", "null")) {
            String json = "{\"item.tactical\":{\"allowed\":[\"minecraft:paper\"],\"limit\":4,\"item_limits\":" + overrides + "}}";
            assertThrows(RuntimeException.class, () -> cap.decode(JsonParser.parseString(json)), overrides);
            assertEquals(before, cap.read());
        }
    }

    @Test void allPanelSlotsHaveStableUniqueIdsAndIndices() {
        assertEquals(17, InventorySlot.ALL.size());
        assertEquals(10, InventoryRules.defaults().slots().size());
        for (var slot : InventorySlot.ALL) {
            assertSame(slot, InventorySlot.byIndex(slot.index()));
            assertSame(slot, InventorySlot.byId(slot.id()));
        }
        for (String oldId : List.of("item.grenade", "item.battery", "item.medkit", "item.tactical.1", "ammo.1", "accessory.1", "weapon.primary")) {
            assertThrows(IllegalArgumentException.class, () -> new InventoryRules(Map.of(oldId,
                    new InventoryRules.Definition(List.of("minecraft:paper"), 4))));
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
        assertThrows(UnsupportedOperationException.class, () -> changed.slots().get("weapon").allowed().clear());
    }

    @Test void malformedUpdatesAreAtomicAndCannotIntroduceUnknownSlots() {
        var cap = new BattlezoneInventoryCapability(null);
        var previous = cap.read();
        for (String json : List.of(
                "{\"weapon\":{\"allowed\":[\"minecraft:stone\"],\"limit\":0}}",
                "{\"weapon.typo\":{\"allowed\":[],\"limit\":1}}",
                "{\"weapon\":{\"allowed\":[\"bad id\"],\"limit\":1}}")) {
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
            var json = JsonParser.parseString("{\"item.tactical\":" + rule + "}");
            var result = InventoryRules.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, json);
            assertTrue(result.error().orElseThrow().message().contains("item.tactical"));
            var failure = assertThrows(RuntimeException.class, () -> cap.decode(json));
            assertTrue(failure.getMessage().contains("item.tactical"), failure.getMessage());
            assertEquals(before, cap.read());
        }
    }

    @Test void categoryRulesApplyToEveryMatchingSlotAfterRoundTrip() {
        var cap = new BattlezoneInventoryCapability(null);
        cap.decode(JsonParser.parseString("""
                {"item.tactical":{"allowed":["minecraft:paper"],"limit":7},
                 "ammo":{"allowed":["minecraft:paper"],"limit":20},
                 "accessory":{"allowed":["*"],"limit":1},
                 "weapon":{"allowed":["@gun:tacz:ak47"],"limit":1}}
                """));
        var restored = new BattlezoneInventoryCapability(null);
        restored.decode(cap.toJson());
        assertEquals(cap.read(), restored.read());
        for (String category : List.of("item.tactical", "ammo", "accessory", "weapon")) {
            var group = InventorySlot.byCategory(category);
            for (var slot : group) assertSame(restored.rule(group.get(0)), restored.rule(slot));
        }
        assertEquals(7, restored.rule(InventorySlot.TACTICAL_1).limit());
        assertEquals(7, restored.rule(InventorySlot.TACTICAL_2).limit());
        assertEquals(20, restored.rule(InventorySlot.AMMO_5).limit());
    }

    @Test void mapsFreezeIndependentMatchRulesUntilReset() {
        var first = new BattlezoneInventoryCapability(null);
        var second = new BattlezoneInventoryCapability(null);
        first.begin();
        var active = first.rule(InventorySlot.TACTICAL_2);
        first.write(new InventoryRules(Map.of("item.tactical", new InventoryRules.Definition(List.of("*"), 3))));
        assertSame(active, first.rule(InventorySlot.TACTICAL_2));
        assertEquals(64, second.rule(InventorySlot.TACTICAL_2).limit());
        first.reset();
        assertEquals(3, first.rule(InventorySlot.TACTICAL_2).limit());
        assertEquals(first.read(), new InventoryRules(first.read().slots()));
    }
}
