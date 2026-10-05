package dev.stya.blockzone.zone;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BoundaryGeometryTest {
    private final BoundaryGeometry bounds = BoundaryGeometry.of(0, 0, 15, 15);

    @Test
    void merelyBeingWithinEightBlocksDoesNotShowPanel() {
        assertFalse(bounds.touchesX(0, 3.7, 4.3, 7.7, 8.3));
        assertFalse(bounds.touchesZ(0, 7.7, 8.3, 3.7, 4.3));
    }

    @Test
    void contactUsesPlayerBoxInsteadOfCentre() {
        assertTrue(bounds.touchesX(0, 0, 0.6, 7.7, 8.3));
        assertTrue(bounds.touchesX(16, 15.4, 16, 7.7, 8.3));
        assertTrue(bounds.touchesZ(0, 7.7, 8.3, 0, 0.6));
        assertTrue(bounds.touchesZ(16, 7.7, 8.3, 15.4, 16));
    }

    @Test
    void cornerContactCanShowBothFaces() {
        assertTrue(bounds.touchesX(0, 0, 0.6, 0, 0.6));
        assertTrue(bounds.touchesZ(0, 0, 0.6, 0, 0.6));
    }

    @Test
    void planeExtensionBeyondMapDoesNotCountAsContact() {
        assertFalse(bounds.touchesX(0, 0, 0.6, 20, 20.6));
        assertFalse(bounds.touchesZ(0, 20, 20.6, 0, 0.6));
    }

    @Test
    void blockCornersAreInclusiveAndCanBeReversed() {
        assertEquals(bounds, BoundaryGeometry.of(15, 15, 0, 0));
        assertEquals(16, bounds.maxX());
        assertEquals(16, bounds.maxZ());
    }
}
