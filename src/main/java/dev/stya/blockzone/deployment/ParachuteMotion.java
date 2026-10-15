package dev.stya.blockzone.deployment;

/** Low glide ratio, gradual turns and a strictly downward terminal speed. Units are blocks/tick. */
public final class ParachuteMotion {
    private ParachuteMotion() { }

    public static Motion step(double x, double y, double z, float yaw, float pitch, int forwardInput, int leftInput) {
        double dive = Math.max(0, Math.min(1, pitch / 90.0));
        double forward = 0.30 - 0.18 * dive;
        double downward = -0.45 - 1.05 * dive;
        var drift = drift(x, z, yaw, forwardInput, leftInput, forward, 0.12);
        // Preserve normal freefall momentum while the canopy brakes it; clamp only abnormal boosts.
        double descent = Math.max(-2.8, Math.min(-0.45, y));
        double nextY = descent + (downward - descent) * 0.10;
        return new Motion(drift.x(), nextY, drift.z());
    }

    public static Motion freefall(double x, double y, double z, float yaw, int forwardInput, int leftInput) {
        var drift = drift(x, z, yaw, forwardInput, leftInput, 0.08, 0.05);
        return new Motion(drift.x(), Math.max(-2.8, Math.min(-0.45, y - 0.12)), drift.z());
    }

    private static Motion drift(double x, double z, float yaw, int forwardInput, int leftInput,
                                double speed, double response) {
        double forward = Math.max(-1, Math.min(1, forwardInput));
        double left = Math.max(-1, Math.min(1, leftInput));
        double length = Math.max(1, Math.hypot(forward, left));
        double angle = Math.toRadians(yaw);
        double targetX = (-Math.sin(angle) * forward + Math.cos(angle) * left) * speed / length;
        double targetZ = (Math.cos(angle) * forward + Math.sin(angle) * left) * speed / length;
        double nextX = x + (targetX - x) * response;
        double nextZ = z + (targetZ - z) * response;
        // Clamp inherited boosts; diagonal input never increases the permitted speed.
        double horizontal = Math.hypot(nextX, nextZ);
        if (horizontal > 0.30) {
            nextX *= 0.30 / horizontal;
            nextZ *= 0.30 / horizontal;
        }
        return new Motion(nextX, 0, nextZ);
    }

    public record Motion(double x, double y, double z) { }
}
