package dev.stya.blockzone.combat;

/** Independent bleed-out and uninterrupted rescue clocks, measured in server ticks. */
public final class RescueTimer {
    public static final int BLEED_TICKS = 1200;
    public static final int RESCUE_TICKS = 160;
    private int remaining = BLEED_TICKS;
    private int progress;
    public boolean tick(boolean rescuing) {
        remaining--;
        progress = rescuing ? progress + 1 : 0;
        return remaining <= 0;
    }
    public void interrupt() { progress = 0; }
    public boolean rescued() { return progress >= RESCUE_TICKS; }
    public int remaining() { return Math.max(0, remaining); }
    public int progress() { return progress; }
}
