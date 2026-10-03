package dev.stya.blockzone.map.battlezone;

/** Low glide ratio, gradual turns and a strictly downward terminal speed. Units are blocks/tick. */
public final class ParachuteMotion {
    private ParachuteMotion() { }

    public static Motion step(double x, double y, double z, float yaw, float pitch) {
        double dive = Math.max(0, Math.min(1, pitch / 90.0));
        double forward = 0.30 - 0.18 * dive;
        double downward = -0.45 - 1.05 * dive;
        double angle = Math.toRadians(yaw);
        double nextX = x + (-Math.sin(angle) * forward - x) * 0.12;
        double nextZ = z + (Math.cos(angle) * forward - z) * 0.12;
        // Avoid inheriting a knockback/boost velocity that defeats the limited glide ratio.
        double horizontal = Math.hypot(nextX, nextZ);
        if (horizontal > 0.30) {
            nextX *= 0.30 / horizontal;
            nextZ *= 0.30 / horizontal;
        }
        double nextY = Math.max(-1.5, Math.min(-0.45, y + (downward - y) * 0.12));
        return new Motion(nextX, nextY, nextZ);
    }

    public static Motion freefall(double x, double y, double z, float yaw) {
        double angle = Math.toRadians(yaw);
        double nextX = x + (-Math.sin(angle) * 0.08 - x) * 0.05;
        double nextZ = z + (Math.cos(angle) * 0.08 - z) * 0.05;
        double horizontal = Math.hypot(nextX, nextZ);
        if (horizontal > 0.30) {
            nextX *= 0.30 / horizontal;
            nextZ *= 0.30 / horizontal;
        }
        return new Motion(nextX, Math.max(-2.8, Math.min(-0.45, y - 0.12)), nextZ);
    }

    public record Motion(double x, double y, double z) { }
}
