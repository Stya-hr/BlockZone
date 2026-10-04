package dev.stya.blockzone.util.editor;

import dev.stya.blockzone.util.battlezone.PoisonPath;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class PoisonEditorDraftTest {
    private final PoisonPath.Circle first = new PoisonPath.Circle(10, 20, 30, 0, 0, 1);
    private final PoisonPath.Circle second = new PoisonPath.Circle(20, 30, 40, 5, 10, 2);
    private PoisonEditorDraft draft() { return new PoisonEditorDraft(List.of(new PoisonPath(List.of(first, second)))); }
    @Test void copiedSequencesEditIndependentlyAndLeaveOriginalUntouched() {
        var source = List.of(new PoisonPath(List.of(first, second)));
        var draft = new PoisonEditorDraft(source); draft.duplicateSequence(); draft.replace(second);
        assertEquals(first, source.get(0).circles().get(0));
        assertEquals(first, draft.paths().get(0).circles().get(0));
        assertEquals(second, draft.paths().get(1).circles().get(0));
    }
    @Test void deletingAndNavigatingKeepsAValidSelectedCircle() {
        var draft = draft(); draft.selectCircle(-1); assertEquals(1, draft.circle());
        assertTrue(draft.removeCircle()); assertEquals(0, draft.circle());
        assertFalse(draft.removeCircle()); assertFalse(draft.removeSequence());
        draft.duplicateSequence(); assertTrue(draft.removeSequence()); assertEquals(0, draft.sequence());
    }
    @Test void reorderingMovesTimingAndDamageWithTheCircleAndPreservesSelection() {
        var draft = draft(); draft.moveCircle(1);
        assertEquals(first, draft.selected()); assertEquals(1, draft.circle());
        assertEquals(second, draft.path().circles().get(0));
        draft.moveCircle(1); assertEquals(1, draft.circle());
        draft.addCircle(); assertEquals(first, draft.selected()); assertEquals(3, draft.path().circles().size());
    }
    @Test void malformedEmptyPathsCannotOpenAnEditor() {
        assertThrows(IllegalArgumentException.class, () -> new PoisonEditorDraft(List.of()));
        assertThrows(IllegalArgumentException.class, () -> new PoisonEditorDraft(List.of(new PoisonPath(List.of()))));
    }
}
