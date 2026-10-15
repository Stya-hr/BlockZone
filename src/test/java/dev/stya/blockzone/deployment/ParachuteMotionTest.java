package dev.stya.blockzone.deployment;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ParachuteMotionTest {
    @Test void directionalInputIsRelativeToViewAndDiagonalsAreNormalized() {
        var forward = ParachuteMotion.step(0, -0.45, 0, 0, 0, 1, 0);
        var backward = ParachuteMotion.step(0, -0.45, 0, 0, 0, -1, 0);
        var left = ParachuteMotion.step(0, -0.45, 0, 0, 0, 0, 1);
        var right = ParachuteMotion.step(0, -0.45, 0, 0, 0, 0, -1);
        assertEquals(-forward.z(), backward.z(), 1e-9);
        assertTrue(left.x() > 0 && right.x() < 0);
        var diagonal = ParachuteMotion.step(0, -0.45, 0, 0, 0, 1, 1);
        assertEquals(Math.hypot(forward.x(), forward.z()), Math.hypot(diagonal.x(), diagonal.z()), 1e-9);
        var turned = ParachuteMotion.step(0, -0.45, 0, 90, 0, 1, 0);
        assertEquals(-forward.z(), turned.x(), 1e-9);
        assertEquals(0, turned.z(), 1e-9);
        var coasting = ParachuteMotion.step(0, -0.45, 0.3, 0, 0, 0, 0);
        assertTrue(coasting.z() > 0 && coasting.z() < 0.3);
        var fallingLeft = ParachuteMotion.freefall(0, -2.8, 0, 0, 0, 1);
        assertTrue(fallingLeft.x() > 0);
        assertEquals(0, fallingLeft.z(), 1e-9);
    }
    @Test void lookingUpNeverProducesLiftOrLongGlide() {
        var motion = new ParachuteMotion.Motion(0, 4, 0);
        for (int i = 0; i < 200; i++) {
            motion = ParachuteMotion.step(motion.x(), motion.y(), motion.z(), 0, -90, 1, 0);
            assertTrue(motion.y() <= -0.16 + 1e-9);
            assertTrue(Math.hypot(motion.x(), motion.z()) <= 0.14 + 1e-9);
        }
        assertEquals(-0.24, motion.y(), 1e-6);
        assertEquals(0.14, motion.z(), 1e-6);
    }

    @Test void divingIsFasterWithLessHorizontalTravelAndTurnsAreGradual() {
        var level = ParachuteMotion.step(0, -0.8, 0.14, 0, 0, 1, 0);
        var dive = ParachuteMotion.step(0, -0.8, 0.14, 0, 90, 1, 0);
        assertTrue(dive.y() < level.y());
        assertTrue(dive.z() < level.z());
        var turn = ParachuteMotion.step(0, -0.8, 0.14, 90, 30, 1, 0);
        assertTrue(turn.x() < 0);
        assertTrue(turn.z() > 0);
    }

    @Test void freefallAcceleratesAndOpeningChuteSlowsDescent() {
        var motion = new ParachuteMotion.Motion(0, -0.8, 0);
        for (int i = 0; i < 100; i++) {
            motion = ParachuteMotion.freefall(motion.x(), motion.y(), motion.z(), 0, 1, 0);
        }
        assertEquals(-2.8, motion.y());
        var opened = ParachuteMotion.step(motion.x(), motion.y(), motion.z(), 0, 0, 1, 0);
        assertTrue(opened.y() > motion.y());
        assertTrue(opened.y() < -2.5, "Opening must preserve momentum instead of snapping to canopy terminal speed");
        var braking = opened;
        for (int i = 1; i < 20; i++) {
            var next = ParachuteMotion.step(braking.x(), braking.y(), braking.z(), 0, 0, 1, 0);
            assertTrue(next.y() > braking.y());
            assertTrue(next.y() < -0.24);
            braking = next;
        }
        assertTrue(braking.y() > -0.8, "Canopy should shed most descent speed over about one second");
        var closed = ParachuteMotion.freefall(opened.x(), opened.y(), opened.z(), 0, 1, 0);
        assertTrue(closed.y() < opened.y());
    }

    @Test void inheritedBoostsAreClampedInBothDirections() {
        var motion = ParachuteMotion.step(10, -10, 10, 45, 45, 1, 0);
        assertEquals(0.14, Math.hypot(motion.x(), motion.z()), 1e-9);
        assertTrue(motion.y() > -2.8 && motion.y() < -1.5);
    }

    @Test void neutralCanopyDescendsSlowlyAndReleaseBleedsOffDrift() {
        var neutral = new ParachuteMotion.Motion(0, -0.16, 0);
        for (int i = 0; i < 100; i++) neutral = ParachuteMotion.step(neutral.x(), neutral.y(), neutral.z(), 0, 0, 0, 0);
        assertEquals(-0.16, neutral.y(), 1e-9);
        assertEquals(0, neutral.x(), 1e-9);
        assertEquals(0, neutral.z(), 1e-9);
        var drift = new ParachuteMotion.Motion(0, -0.24, .14);
        var reversed = ParachuteMotion.step(drift.x(), drift.y(), drift.z(), 0, 0, -1, 0);
        assertTrue(reversed.z() > 0, "Direction changes retain inertia instead of instantly reversing");
        for (int i = 0; i < 20; i++) drift = ParachuteMotion.step(drift.x(), drift.y(), drift.z(), 0, 0, 0, 0);
        assertTrue(drift.z() > 0 && drift.z() < .05);
    }
}
