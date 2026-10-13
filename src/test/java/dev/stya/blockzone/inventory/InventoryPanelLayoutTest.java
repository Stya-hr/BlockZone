package dev.stya.blockzone.inventory;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class InventoryPanelLayoutTest {
    @Test void firstRowIsTwoAccessoriesAndFourNativeArmorParts() {
        assertEquals(InventorySlot.FIRST_ROW, InventoryPanelLayout.CELLS.subList(0, 6).stream().map(InventoryPanelLayout.Cell::slot).toList());
        assertEquals(java.util.List.of(net.minecraft.world.entity.EquipmentSlot.HEAD, net.minecraft.world.entity.EquipmentSlot.CHEST,
                net.minecraft.world.entity.EquipmentSlot.LEGS, net.minecraft.world.entity.EquipmentSlot.FEET),
                InventorySlot.ARMOR.stream().map(InventorySlot::equipmentSlot).toList());
    }
    @Test void allSlotsHaveOneDisjointClickableRegion() {
        assertEquals(17, InventoryPanelLayout.CELLS.size());
        assertEquals(17, InventoryPanelLayout.CELLS.stream().map(InventoryPanelLayout.Cell::slot).distinct().count());
        for (var cell : InventoryPanelLayout.CELLS) {
            assertSame(cell, InventoryPanelLayout.at(cell.x() + cell.width() / 2., cell.y() + cell.height() / 2.));
            for (var other : InventoryPanelLayout.CELLS) {
                if (cell == other) continue;
                assertTrue(cell.x() + cell.width() <= other.x() || other.x() + other.width() <= cell.x()
                        || cell.y() + cell.height() <= other.y() || other.y() + other.height() <= cell.y());
            }
        }
    }
    @Test void HeadersGapsAndFooterCannotDiscardItems() {
        assertNull(InventoryPanelLayout.at(520, 14));
        assertNull(InventoryPanelLayout.at(235, 170));
        assertNull(InventoryPanelLayout.at(520, 390));
        assertNull(InventoryPanelLayout.at(-1, 170));
        assertNull(InventoryPanelLayout.at(1040, 170));
    }
}
