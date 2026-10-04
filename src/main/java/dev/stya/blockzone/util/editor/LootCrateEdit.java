package dev.stya.blockzone.util.editor;

/** Serializable editor data, independent of client/world classes. */
public record LootCrateEdit(int x, int y, int z, String table, long seed, boolean opened) {
    public static final int MAX_CRATES = 2048;
    public record Position(int x, int y, int z) { }
    public Position position() { return new Position(x, y, z); }
}
