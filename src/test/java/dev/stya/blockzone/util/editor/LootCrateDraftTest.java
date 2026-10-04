package dev.stya.blockzone.util.editor;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class LootCrateDraftTest {
    private final LootCrateEdit a = new LootCrateEdit(1, 2, 3, "demo:a", 7, true);
    private final LootCrateEdit b = new LootCrateEdit(4, 5, 6, "demo:b", 8, false);
    @Test void filteringDropsHiddenSelectionAndSelectsOnlyVisible() {
        var draft = new LootCrateDraft(List.of(a, b)); draft.selectAll(); draft.filter("demo:a");
        assertEquals(0, draft.selectedCount()); draft.selectAll(); draft.apply("demo:c", 9, true);
        assertEquals(List.of(new LootCrateEdit(1, 2, 3, "demo:c", 9, false)), draft.changes());
        assertEquals(b, draft.entries().get(1));
    }
    @Test void reversedBoxIncludesBoundariesAndSupportsAdding() {
        var draft = new LootCrateDraft(List.of(a, b)); draft.selectBox(1, 3, 0, 0, false);
        assertTrue(draft.selected(a)); assertFalse(draft.selected(b));
        draft.selectBox(5, 7, 4, 6, true); assertEquals(2, draft.selectedCount());
        draft.selectBox(20, 20, 21, 21, false); assertEquals(0, draft.selectedCount());
    }
    @Test void invalidInputDoesNotPartiallyApplyAndSavedDraftBecomesBaseline() {
        var draft = new LootCrateDraft(List.of(a, b)); draft.selectAll();
        assertThrows(IllegalArgumentException.class, () -> draft.apply("INVALID", 9, true)); assertFalse(draft.dirty());
        draft.apply("chests/simple_dungeon", -20, false);
        assertTrue(draft.entries().get(0).opened()); assertEquals("minecraft:chests/simple_dungeon", draft.entries().get(0).table());
        draft.markSaved(); assertFalse(draft.dirty());
        draft.clearSelection(); assertThrows(IllegalArgumentException.class, () -> draft.apply("demo:c", 9, true));
    }
    @Test void duplicatePositionsAreRejectedAndEntriesCannotBeMutated() {
        assertThrows(IllegalArgumentException.class, () -> new LootCrateDraft(List.of(a, a)));
        var draft = new LootCrateDraft(List.of(a)); assertThrows(UnsupportedOperationException.class, () -> draft.entries().clear());
    }
}
