package dev.stya.blockzone.client.hud.squad;

/** Fixed-size rows: grow upward, then right, then paginate. The local row never moves. */
public record SquadRosterLayout(int rows, int columns, int pages, int count) {
    public static final float COLUMN_STEP = 220, TOP_SAFE = 360;
    public static SquadRosterLayout create(int teammates, float localTop, float viewportWidth) {
        int rows = Math.max(1, (int)Math.floor((localTop - 42 - TOP_SAFE) / SquadHudStyle.TEAM_STEP) + 1);
        int columns = Math.max(1, Math.min(2, (int)((viewportWidth * .4f - SquadHudStyle.LEFT) / COLUMN_STEP)));
        int capacity = rows * columns;
        return new SquadRosterLayout(rows, columns, Math.max(1, (Math.max(0, teammates) - 1) / capacity + 1), Math.max(0, teammates));
    }
    public int capacity() { return rows * columns; }
    public int start(int page) { return Math.max(0, Math.min(pages - 1, page)) * capacity(); }
    public int end(int page) { return Math.min(count, start(page) + capacity()); }
    public float x(int slot) { return (slot / rows) * COLUMN_STEP; }
    public float y(int slot) { return -42 - (slot % rows) * SquadHudStyle.TEAM_STEP; }
}
