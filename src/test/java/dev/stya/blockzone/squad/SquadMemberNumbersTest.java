package dev.stya.blockzone.squad;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SquadMemberNumbersTest {
    @Test void lateJoinsAndIterationOrderDoNotChangeExistingColors() {
        var a = new UUID(0, 2); var b = new UUID(0, 3); var late = new UUID(0, 1);
        var numbers = new SquadMemberNumbers();
        numbers.update(List.of(a, b));
        numbers.update(List.of(late, b, a));
        assertEquals(1, numbers.number(a));
        assertEquals(2, numbers.number(b));
        assertEquals(3, numbers.number(late));
    }
    @Test void departedMembersReleaseTheirNumberWithoutRenumberingSurvivors() {
        var a = new UUID(0, 1); var b = new UUID(0, 2); var c = new UUID(0, 3);
        var numbers = new SquadMemberNumbers();
        numbers.update(List.of(a, b)); numbers.update(List.of(b, c));
        assertEquals(0, numbers.number(a));
        assertEquals(2, numbers.number(b));
        assertEquals(1, numbers.number(c));
    }
}
