package dev.stya.blockzone.client.hud.squad;

import java.util.HashSet;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SquadRosterLayoutTest {
    @Test void referenceThreePersonSquadKeepsOriginalCoordinates() {
        var layout = SquadRosterLayout.create(2, 1256);
        assertEquals(1, layout.columns());
        assertEquals(0, layout.x(0));
        assertEquals(-42, layout.y(0));
        assertEquals(-86, layout.y(1));
    }

    @Test void everyTeammateIsVisibleAndRowsGrowBeforeColumns() {
        var layout = SquadRosterLayout.create(23, 1256);
        assertEquals(20, layout.rows());
        assertEquals(2, layout.columns());
        assertEquals(23, layout.count());
        assertEquals(220, layout.x(20));
        assertEquals(-42, layout.y(20));
    }

    @Test void largeRosterHasNoDuplicateOrClippedRows() {
        var layout = SquadRosterLayout.create(100, 1256);
        var positions = new HashSet<String>();
        for (int slot = 0; slot < layout.count(); slot++) {
            assertTrue(positions.add(layout.x(slot) + ":" + layout.y(slot)));
            assertTrue(1256 + layout.y(slot) >= SquadRosterLayout.TOP_SAFE);
        }
        assertEquals(layout.count(), positions.size());
    }

    @Test void emptyRosterHasNoRowsToRender() {
        var layout = SquadRosterLayout.create(0, 1256);
        assertEquals(0, layout.count());
        assertEquals(1, layout.rows());
    }
}
