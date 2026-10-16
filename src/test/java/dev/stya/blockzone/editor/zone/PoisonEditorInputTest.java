package dev.stya.blockzone.editor.zone;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PoisonEditorInputTest {
    @Test void rejectsNonFiniteAndOutOfRangeFieldsWithoutRejectingValidCoordinates() {
        assertTrue(PoisonEditorInput.valid(0, "-123.5"));
        assertTrue(PoisonEditorInput.valid(2, "0"));
        assertTrue(PoisonEditorInput.valid(4, "1000000"));
        assertFalse(PoisonEditorInput.valid(0, "NaN"));
        assertFalse(PoisonEditorInput.valid(1, "Infinity"));
        assertFalse(PoisonEditorInput.valid(2, "-1"));
        assertFalse(PoisonEditorInput.valid(2, "30000001"));
        assertFalse(PoisonEditorInput.valid(3, "1.5"));
        assertFalse(PoisonEditorInput.valid(4, "1000001"));
        assertFalse(PoisonEditorInput.valid(5, "-1"));
        assertFalse(PoisonEditorInput.valid(5, "1e40"));
        assertFalse(PoisonEditorInput.valid(0, ""));
    }
}
