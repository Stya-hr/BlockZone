package dev.stya.blockzone.client.hud.squad;

/** Frame-rate independent foreground easing with a delayed damage trail. Times are seconds. */
final class VitalBarMotion {
    private float target, value, trail;
    private double updatedAt, trailStartsAt;
    VitalBarMotion(float initial, double now) {
        target = value = trail = initial;
        updatedAt = trailStartsAt = now;
    }
    void update(float next, double now) {
        double dt = Math.max(0, now - updatedAt);
        if (next > target) {
            // Healing and plating must never leave a damage-colored remainder.
            target = value = trail = next;
            trailStartsAt = now;
        } else {
            if (next < target) {
                trail = Math.max(trail, value);
                trailStartsAt = now + .25;
                target = next;
            }
            value = approach(value, target, dt, .075);
            double trailDt = Math.max(0, now - Math.max(updatedAt, trailStartsAt));
            trail = Math.max(value, approach(trail, target, trailDt, .3));
        }
        updatedAt = now;
    }
    private static float approach(float from, float to, double dt, double duration) {
        float result = to + (from - to) * (float)Math.exp(-dt / duration);
        return Math.abs(result - to) < .001f ? to : result;
    }
    float value() { return value; }
    float trail() { return trail; }
}
