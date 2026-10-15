package dev.stya.blockzone.client.hud.squad;

import dev.stya.blockzone.squad.SquadMemberStatus;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SquadVitalsAnimationTest {
    private final UUID id = UUID.randomUUID();
    private SquadMemberStatus member(float hp, float armor) {
        return new SquadMemberStatus(id, "Ally", 1, true, true, hp, 100, armor, 50, 2, 0, 0, 0, 0, 0, false, false);
    }
    @Test void healthAndAbsorbedDamageFlashButHealingAndFirstSnapshotDoNot() {
        var animation = new SquadVitalsAnimation();
        animation.update(List.of(member(100, 100)), "map/team", 0);
        assertEquals(Double.NEGATIVE_INFINITY, animation.get(id).hitAt);
        animation.update(List.of(member(100, 75)), "map/team", .1);
        assertEquals(.1, animation.get(id).hitAt);
        animation.update(List.of(member(80, 75)), "map/team", .2);
        assertEquals(.2, animation.get(id).hitAt);
        animation.update(List.of(member(100, 100)), "map/team", .3);
        assertEquals(.2, animation.get(id).hitAt);
    }
    @Test void switchingTeamOrRemovingMemberClearsOldFlashAndDamageTrail() {
        var animation = new SquadVitalsAnimation();
        animation.update(List.of(member(100, 100)), "map/a", 0);
        animation.update(List.of(member(50, 100)), "map/a", .1);
        animation.update(List.of(member(50, 100)), "map/b", .2);
        assertEquals(Double.NEGATIVE_INFINITY, animation.get(id).hitAt);
        assertEquals(.5f, animation.get(id).health.trail());
        animation.update(List.of(), "map/b", .3);
        assertNull(animation.get(id));
    }
}
