package dev.stya.blockzone.combat;

/** Independent bleed-out and uninterrupted rescue clocks, measured in server ticks. */
public final class RescueTimer {
    private final int bleedTicks;
    private final int rescueTicks;
    private int remaining;
    public RescueTimer(int bleedTicks, int rescueTicks) {
        if (bleedTicks < 1 || rescueTicks < 1) throw new IllegalArgumentException("Invalid rescue duration");
        this.bleedTicks = bleedTicks;
        this.rescueTicks = rescueTicks;
        this.remaining = bleedTicks;
    }
    public int bleedTicks() { return bleedTicks; }
    public int rescueTicks() { return rescueTicks; }
    private int progress;
    public boolean tick(boolean rescuing) {
        remaining--;
        progress = rescuing ? progress + 1 : 0;
        return remaining <= 0;
    }
    public void interrupt() { progress = 0; }
    public boolean rescued() { return progress >= rescueTicks; }
    public int remaining() { return Math.max(0, remaining); }
    public int progress() { return progress; }
}
