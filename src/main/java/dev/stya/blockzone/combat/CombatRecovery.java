package dev.stya.blockzone.combat;

/** Tick-based recovery clock; armor hits also restart the delay. */
public final class CombatRecovery {
    public static final float MAX_HEALTH = 100;
    public static final float PLATE_POINTS = 50;
    private final int delayTicks;
    private final int intervalTicks;
    private int quietTicks;
    public CombatRecovery(int delayTicks, int intervalTicks) {
        if (delayTicks < 0 || intervalTicks < 1) throw new IllegalArgumentException("Invalid recovery duration");
        this.delayTicks = delayTicks;
        this.intervalTicks = intervalTicks;
    }

    public void hurt() { quietTicks = 0; }

    public boolean tick() {
        if (quietTicks < delayTicks + intervalTicks) quietTicks++;
        if (quietTicks < delayTicks + intervalTicks) return false;
        quietTicks = delayTicks;
        return true;
    }

    public static float insertPlate(float armor, float platePoints) {
        return Math.min(platePoints * 3, Math.max(0, armor) + platePoints);
    }
}
