package dev.stya.blockzone.client.hud.squad;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class VitalBarMotionTest {
    @Test void damageLeavesATrailThenSettlesAtTheNewValue() {
        var motion = new VitalBarMotion(1, 0);
        motion.update(.4f, .01);
        motion.update(.4f, .2);
        assertTrue(motion.value() > .4f && motion.value() < .5f);
        assertEquals(1, motion.trail());
        motion.update(.4f, .5);
        assertTrue(motion.trail() > motion.value() && motion.trail() < 1);
        motion.update(.4f, 4);
        assertEquals(.4f, motion.value());
        assertEquals(.4f, motion.trail());
    }
    @Test void repeatedHitsHoldTheRemainingTrailAndHealingClearsIt() {
        var motion = new VitalBarMotion(1, 0);
        motion.update(.7f, .01);
        motion.update(.7f, .3);
        float trail = motion.trail();
        motion.update(.2f, .31);
        motion.update(.2f, .5);
        assertEquals(trail, motion.trail());
        motion.update(.8f, .51);
        assertEquals(.8f, motion.value());
        assertEquals(.8f, motion.trail());
    }
    @Test void easingDoesNotDependOnFrameRate() {
        var fast = new VitalBarMotion(1, 0);
        var slow = new VitalBarMotion(1, 0);
        fast.update(.2f, 0);
        slow.update(.2f, 0);
        for (int i = 1; i <= 120; i++) fast.update(.2f, i / 120.0);
        for (int i = 1; i <= 20; i++) slow.update(.2f, i / 20.0);
        assertEquals(fast.value(), slow.value(), .001f);
        assertEquals(fast.trail(), slow.trail(), .001f);
    }
}
