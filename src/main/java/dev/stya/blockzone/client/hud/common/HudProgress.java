package dev.stya.blockzone.client.hud.common;

/** Shared, read-only timer conversion. Callers supply server ticks or native item-use ticks. */
public record HudProgress(float fraction, float seconds) {
    public static HudProgress elapsed(double elapsed, double total, double ticksPerSecond) {
        if (!Double.isFinite(total) || total <= 0 || !Double.isFinite(ticksPerSecond) || ticksPerSecond <= 0)
            return new HudProgress(0, 0);
        double done = Double.isFinite(elapsed) ? Math.max(0, Math.min(total, elapsed)) : 0;
        return new HudProgress((float)(done / total), (float)((total - done) / ticksPerSecond));
    }
    public static HudProgress remaining(double remaining, double total, double ticksPerSecond) {
        if (!Double.isFinite(remaining) || !Double.isFinite(total) || total <= 0
                || !Double.isFinite(ticksPerSecond) || ticksPerSecond <= 0) return new HudProgress(0, 0);
        var value = elapsed(total - remaining, total, ticksPerSecond);
        return new HudProgress(1 - value.fraction, value.seconds);
    }
}
