package dev.stya.blockzone.client.hud.squad;

/** Fixed-size rows: grow upward, then right. The local row never moves. */
public record SquadRosterLayout(int rows, int columns, int count) {
    public static final float COLUMN_STEP = 220, TOP_SAFE = 360;
    public static SquadRosterLayout create(int teammates, float localTop) {
        int count = Math.max(0, teammates);
        int maxRows = Math.max(1, (int)Math.floor((localTop - 42 - TOP_SAFE) / SquadHudStyle.TEAM_STEP) + 1);
        int rows = Math.max(1, Math.min(count, maxRows));
        int columns = Math.max(1, (count + rows - 1) / rows);
        return new SquadRosterLayout(rows, columns, count);
    }
    public float x(int slot) { return (slot / rows) * COLUMN_STEP; }
    public float y(int slot) { return -42 - (slot % rows) * SquadHudStyle.TEAM_STEP; }
}
