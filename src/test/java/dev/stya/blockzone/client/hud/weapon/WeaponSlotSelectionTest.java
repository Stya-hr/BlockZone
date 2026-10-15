package dev.stya.blockzone.client.hud.weapon;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WeaponSlotSelectionTest {
    @Test void wheelWrapsBothDirectionsWithoutSelectingHiddenSlots() {
        assertEquals(4, WeaponSlotSelection.next(0, 1));
        assertEquals(0, WeaponSlotSelection.next(4, -1));
        int selected = 0;
        for (int i = 0; i < 5; i++) selected = WeaponSlotSelection.next(selected, -1);
        assertEquals(0, selected);
        for (int i = 0; i < 5; i++) selected = WeaponSlotSelection.next(selected, 1);
        assertEquals(0, selected);
        assertEquals(1, WeaponSlotSelection.next(2, 3.5));
        assertEquals(3, WeaponSlotSelection.next(2, -3.5));
    }
}
