package dev.stya.blockzone.client.hud.notice;

/** One envelope for the background, rules and text; no independent row fading. */
public record AnnouncementLifetime(long startedAt, long durationMillis) {
    public float opacity(long now) {
        long elapsed = now - startedAt;
        if (elapsed < 0 || elapsed >= durationMillis) return 0;
        float t = Math.min(1, Math.min(elapsed / 120f, (durationMillis - elapsed) / 220f));
        return t * t * (3 - 2 * t);
    }
}
