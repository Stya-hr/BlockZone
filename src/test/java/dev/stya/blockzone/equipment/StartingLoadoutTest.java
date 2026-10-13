package dev.stya.blockzone.equipment;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StartingLoadoutTest {
    @Test void onlyFormalSlotIdsAreAccepted() {
        for (var slot : dev.stya.blockzone.inventory.InventorySlot.ALL)
            assertEquals(slot, StartingLoadout.panelSlot(slot.id()));
    }
    @Test void legacySlotsAreRejected() {
        for (String id : java.util.List.of("head", "chest", "legs", "feet", "offhand", "hotbar.0", "inventory.0", "equipment.backpack", "equipment.shield"))
            assertThrows(IllegalArgumentException.class, () -> StartingLoadout.panelSlot(id));
    }
    @Test void gunNbtAndCountSurviveConfigurationRoundTrip() {
        var json = JsonParser.parseString("""
                {"slot":"weapon.primary","item":"tacz:modern_kinetic_gun","count":1,
                 "nbt":"{GunId:\\"tacz:ak47\\",GunCurrentAmmoCount:30}"}
                """);
        var entry = StartingLoadout.Entry.CODEC.parse(JsonOps.INSTANCE, json).result().orElseThrow();
        assertEquals(json, StartingLoadout.Entry.CODEC.encodeStart(JsonOps.INSTANCE, entry).result().orElseThrow());
    }
    @Test void configRejectsInvalidCounts() {
        assertTrue(StartingLoadout.Entry.CODEC.parse(JsonOps.INSTANCE,
                JsonParser.parseString("{\"slot\":\"chest\",\"item\":\"blockzone:plate_carrier\",\"count\":0}")).error().isPresent());
    }
}
