package dev.stya.blockzone.map.battlezone;

/** Tick-based recovery clock; armor hits also restart the delay. */
public final class CombatRecovery {
    public static final int DELAY_TICKS = 100;
    public static final int INTERVAL_TICKS = 5;
    public static final float MAX_HEALTH = 100;
    public static final float HEAL_POINTS = 5;
    public static final float PLATE_POINTS = 50;
    public static final float MAX_ARMOR = 150;
    private int quietTicks;

    public void hurt() { quietTicks = 0; }

    public boolean tick() {
        if (quietTicks < DELAY_TICKS + INTERVAL_TICKS) quietTicks++;
        if (quietTicks < DELAY_TICKS + INTERVAL_TICKS) return false;
        quietTicks = DELAY_TICKS;
        return true;
    }

    public static float insertPlate(float armor) {
        return Math.min(MAX_ARMOR, Math.max(0, armor) + PLATE_POINTS);
    }
}
