package dev.stya.blockzone.equipment;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PlateCapacityTest {
    @Test void twoSlotCarrierCapsPartialRepairsAtTwoPlates() {
        assertEquals(50, PlateCapacity.insert(0, 50, 2));
        assertEquals(100, PlateCapacity.insert(80, 50, 2));
    }
    @Test void expansionAddsCapacityWithoutRefillingIt() {
        assertEquals(130, PlateCapacity.insert(80, 50, 3));
        assertEquals(150, PlateCapacity.insert(130, 50, 3));
        assertEquals(100, PlateCapacity.insert(150, 50, 2));
    }
    @Test void unequippedCarrierCannotStoreArmor() {
        assertEquals(0, PlateCapacity.insert(75, 50, 0));
        assertEquals(50, PlateCapacity.insert(-20, 50, 2));
    }
    @Test void configuredPlatePointsScaleBothCapacities() {
        assertEquals(50, PlateCapacity.insert(40, 25, 2));
        assertEquals(75, PlateCapacity.insert(65, 25, 3));
    }
}
