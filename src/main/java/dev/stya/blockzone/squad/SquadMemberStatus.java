package dev.stya.blockzone.squad;

import java.util.UUID;

/** Immutable server snapshot; independent of loaded client entities. */
public record SquadMemberStatus(UUID id, String name, int number, boolean online, boolean living,
                                float health, float maxHealth, float armor, float platePoints, int plateSlots,
                                int rescueState, int bleedTicks, int bleedTotalTicks,
                                int rescueTicks, int rescueTotalTicks, boolean plating, boolean firing) {
    public SquadMemberStatus {
        maxHealth = positive(maxHealth);
        health = clamp(health, maxHealth);
        platePoints = positive(platePoints);
        plateSlots = Math.max(0, Math.min(3, plateSlots));
        armor = clamp(armor, platePoints * plateSlots);
    }
    private static float positive(float value) { return Float.isFinite(value) && value > 0 ? value : 1; }
    private static float clamp(float value, float max) { return Float.isFinite(value) ? Math.max(0, Math.min(value, max)) : 0; }
    public float healthFraction() { return living && online ? health / maxHealth : 0; }
    public float armorFraction(int segment) {
        return living && online && segment >= 0 && segment < plateSlots
                ? clamp((armor - segment * platePoints) / platePoints, 1) : 0;
    }
    public boolean downed() { return living && rescueState == 1; }
    public float downedFraction() {
        return rescueTicks > 0 ? clamp(rescueTicks / (float)Math.max(1, rescueTotalTicks), 1)
                : clamp(bleedTicks / (float)Math.max(1, bleedTotalTicks), 1);
    }
}
