package dev.stya.blockzone.deployment;

/** Keeps transport motion continuous when a new server snapshot corrects its clock. */
public final class FlightTimeline {
    private double receivedAt;
    private double elapsed;
    private double correction;
    private double correctionDuration;

    public void update(long elapsedTicks, double clientTime, boolean reset) {
        double previous = ticks(clientTime);
        elapsed = elapsedTicks;
        receivedAt = clientTime;
        correction = reset ? 0 : previous - elapsedTicks;
        // Large delays take longer to reconcile so the aircraft never moves backwards.
        correctionDuration = Math.max(5, Math.abs(correction) * 2);
    }

    public double ticks(double clientTime) {
        double age = Math.max(0, clientTime - receivedAt);
        double remaining = correctionDuration > 0 ? Math.max(0, 1 - age / correctionDuration) : 0;
        return elapsed + age + correction * remaining;
    }
}
