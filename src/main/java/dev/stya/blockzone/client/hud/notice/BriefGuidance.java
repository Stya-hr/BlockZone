package dev.stya.blockzone.client.hud.notice;

/** Once per match scope, starting when the player can actually begin looting. */
public final class BriefGuidance {
    private String scope = "";
    private long startedAt = -1;
    public boolean visible(String nextScope, boolean eligible, long now) {
        if (!scope.equals(nextScope)) { scope = nextScope; startedAt = -1; }
        if (!eligible) return false;
        if (startedAt < 0) startedAt = now;
        return now - startedAt < 4000;
    }
}
