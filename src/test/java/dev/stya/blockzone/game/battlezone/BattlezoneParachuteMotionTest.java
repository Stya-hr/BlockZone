package dev.stya.blockzone.game.battlezone;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BattlezoneParachuteMotionTest {
    @Test void lookingUpNeverProducesLiftOrLongGlide() {
        var motion = new BattlezoneParachuteMotion.Motion(0, 4, 0);
        for (int i = 0; i < 200; i++) {
            motion = BattlezoneParachuteMotion.step(motion.x(), motion.y(), motion.z(), 0, -90);
            assertTrue(motion.y() <= -0.45);
            assertTrue(Math.hypot(motion.x(), motion.z()) <= 0.30 + 1e-9);
        }
        assertEquals(-0.45, motion.y(), 1e-6);
        assertEquals(0.30, motion.z(), 1e-6);
    }

    @Test void divingIsFasterWithLessHorizontalTravelAndTurnsAreGradual() {
        var level = BattlezoneParachuteMotion.step(0, -0.8, 0.3, 0, 0);
        var dive = BattlezoneParachuteMotion.step(0, -0.8, 0.3, 0, 90);
        assertTrue(dive.y() < level.y());
        assertTrue(dive.z() < level.z());
        var turn = BattlezoneParachuteMotion.step(0, -0.8, 0.3, 90, 30);
        assertTrue(turn.x() < 0);
        assertTrue(turn.z() > 0);
    }

    @Test void freefallAcceleratesAndOpeningChuteSlowsDescent() {
        var motion = new BattlezoneParachuteMotion.Motion(0, -0.8, 0);
        for (int i = 0; i < 100; i++) {
            motion = BattlezoneParachuteMotion.freefall(motion.x(), motion.y(), motion.z(), 0);
        }
        assertEquals(-2.8, motion.y());
        var opened = BattlezoneParachuteMotion.step(motion.x(), motion.y(), motion.z(), 0, 0);
        assertTrue(opened.y() > motion.y());
        var closed = BattlezoneParachuteMotion.freefall(opened.x(), opened.y(), opened.z(), 0);
        assertTrue(closed.y() < opened.y());
    }

    @Test void inheritedBoostsAreClampedInBothDirections() {
        var motion = BattlezoneParachuteMotion.step(10, -10, 10, 45, 45);
        assertEquals(0.30, Math.hypot(motion.x(), motion.z()), 1e-9);
        assertEquals(-1.5, motion.y());
    }
}
