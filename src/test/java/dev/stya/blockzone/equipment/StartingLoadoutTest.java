package dev.stya.blockzone.equipment;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StartingLoadoutTest {
    @Test void canonicalDestinationsUnifyNativeAndPanelAliases() {
        assertEquals(StartingLoadout.panelSlot("head"), StartingLoadout.panelSlot("equipment.helmet"));
        assertEquals(StartingLoadout.panelSlot("legs"), StartingLoadout.panelSlot("equipment.legs"));
        assertEquals(StartingLoadout.panelSlot("hotbar.0"), StartingLoadout.panelSlot("weapon.primary"));
        assertEquals(StartingLoadout.panelSlot("hotbar.5"), StartingLoadout.panelSlot("item.armor_plates"));
        assertEquals(dev.stya.blockzone.inventory.InventorySlot.ACCESSORY_2, StartingLoadout.panelSlot("accessory.2"));
    }
    @Test void inventoryAndHotbarSlotsDoNotOverlap() {
        assertEquals(0, StartingLoadout.inventoryIndex("hotbar.0"));
        assertEquals(8, StartingLoadout.inventoryIndex("hotbar.8"));
        assertEquals(9, StartingLoadout.inventoryIndex("inventory.0"));
        assertEquals(35, StartingLoadout.inventoryIndex("inventory.26"));
        assertEquals(-1, StartingLoadout.inventoryIndex("hotbar.9"));
        assertEquals(-1, StartingLoadout.inventoryIndex("inventory.27"));
        assertEquals(-1, StartingLoadout.inventoryIndex("inventory.invalid"));
    }
    @Test void gunNbtAndCountSurviveConfigurationRoundTrip() {
        var json = JsonParser.parseString("""
                {"slot":"hotbar.0","item":"tacz:modern_kinetic_gun","count":1,
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
