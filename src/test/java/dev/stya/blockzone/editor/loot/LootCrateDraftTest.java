package dev.stya.blockzone.editor.loot;

import java.util.List;
import org.junit.jupiter.api.Test;
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
    @Test void ordinaryContainersRequireExplicitEnableAndDisablingClearsConsumption() {
        var normal=new LootCrateEdit(10,2,20,"demo:a",4,false,false,"demo:storage_box");
        var draft=new LootCrateDraft(List.of(normal)); draft.selectAll();
        draft.apply("demo:a",4,true,false); assertFalse(draft.dirty());
        draft.apply("demo:weapons",42,true,true);
        var enabled=draft.changes().get(0);
        assertTrue(enabled.enabled()); assertFalse(enabled.opened());
        assertEquals("demo:storage_box",enabled.block()); assertEquals(42,enabled.seed());
        draft.markSaved(); draft.apply("demo:weapons",42,false,false);
        assertFalse(draft.changes().get(0).enabled()); assertFalse(draft.changes().get(0).opened());
    }
    @Test void blockIdsCanBeFilteredAndBulkConversionLeavesOtherContainersUntouched() {
        var normal=new LootCrateEdit(10,2,20,"demo:a",4,false,false,"demo:storage_box");
        var draft=new LootCrateDraft(List.of(a,normal)); draft.filter("demo:storage_box"); draft.selectAll();
        draft.apply("demo:weapons",42,true,true);
        assertEquals(a,draft.entries().get(0)); assertEquals(1,draft.changes().size());
        assertEquals(normal.position(),draft.changes().get(0).position());
    }
    @Test void consumedContainersCanReturnToNormalWithoutKeepingOpenedState() {
        var consumed=new LootCrateEdit(10,2,20,"demo:a",4,true,true,"demo:storage_box");
        var draft=new LootCrateDraft(List.of(consumed)); draft.selectAll(); draft.apply("demo:a",4,false,false);
        assertFalse(draft.changes().get(0).enabled()); assertFalse(draft.changes().get(0).opened());
    }
}
