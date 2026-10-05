package dev.stya.blockzone.zone;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PoisonZoneStateTest {
    private PoisonZoneState state(int waitSeconds, int shrinkSeconds) {
        var path = new PoisonPath(List.of(new PoisonPath.Circle(50, 50, 40, 0, 0, 1),
                new PoisonPath.Circle(60, 40, 20, waitSeconds, shrinkSeconds, 3)));
        var state = new PoisonZoneState();
        state.initialize(path, path.resolve(BoundaryGeometry.of(0, 0, 100, 100), 0), 2);
        return state;
    }

    @Test void waitsThenInterpolatesAndSwitchesDamageWhenTransitionCompletes() {
        var state = state(1, 1);
        for (int i = 0; i < 20; i++) state.tick();
        assertEquals(40, state.current(0).radius());
        assertEquals(50, state.current(0).centerX());
        assertEquals(2, state.damage());
        for (int i = 0; i < 10; i++) state.tick();
        assertEquals(30, state.current(0).radius());
        assertEquals(55, state.current(0).centerX());
        assertEquals(45, state.current(0).centerZ());
        assertEquals(2, state.damage());
        for (int i = 0; i < 10; i++) state.tick();
        assertEquals(20, state.current(0).radius());
        assertEquals(60, state.current(0).centerX());
        assertEquals(6, state.damage());
        for (int i = 0; i < 100; i++) state.tick();
        assertEquals(20, state.current(0).radius());
        assertEquals(6, state.damage());
    }

    @Test void zeroDurationTransitionCompletesOnItsFirstTick() {
        var state = state(0, 0);
        assertEquals(40, state.current(0).radius());
        state.tick();
        assertEquals(20, state.current(0).radius());
        assertEquals(6, state.damage());
    }

    @Test void restartClearsTransitionProgressAndDamagePhase() {
        var state = state(0, 1);
        for (int i = 0; i < 20; i++) state.tick();
        state.restart();
        assertEquals(40, state.current(0).radius());
        assertEquals(2, state.damage());
        state.tick();
        assertEquals(39, state.current(0).radius());
    }

    @Test void nextTransitionStartsFromThePreviousTarget() {
        var path = new PoisonPath(List.of(new PoisonPath.Circle(50, 50, 40, 0, 0),
                new PoisonPath.Circle(60, 40, 20, 0, 0), new PoisonPath.Circle(65, 35, 10, 1, 1)));
        var state = new PoisonZoneState();
        state.initialize(path, path.resolve(BoundaryGeometry.of(0, 0, 100, 100), 0), 1);
        state.tick();
        for (int i = 0; i < 20; i++) state.tick();
        assertEquals(20, state.current(0).radius());
        assertEquals(60, state.current(0).centerX());
        for (int i = 0; i < 10; i++) state.tick();
        assertEquals(15, state.current(0).radius());
        assertEquals(62.5, state.current(0).centerX());
    }
}
