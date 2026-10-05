package dev.stya.blockzone.editor.loot;



/** Serializable editor data, independent of client/world classes. */
public record LootCrateEdit(int x, int y, int z, String table, long seed, boolean opened, boolean enabled, String block) {
    public LootCrateEdit(int x,int y,int z,String table,long seed,boolean opened) {
        this(x,y,z,table,seed,opened,true,"");
    }
    public static final int MAX_CRATES = 2048;
    public record Position(int x, int y, int z) { }
    public Position position() { return new Position(x, y, z); }
}
