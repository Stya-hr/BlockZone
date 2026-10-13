package dev.stya.blockzone.inventory;

import java.util.ArrayList;
import java.util.List;

/** Shared rendering/hit-test geometry in the 1040 x 410 design space. */
public final class InventoryPanelLayout {
    public record Cell(InventorySlot slot, int x, int y, int width, int height) {
        public boolean contains(double px, double py) { return px >= x && py >= y && px < x + width && py < y + height; }
    }
    public static final List<Cell> CELLS;
    static {
        var cells = new ArrayList<Cell>();
        for (int i = 0; i < 6; i++) cells.add(new Cell(InventorySlot.FIRST_ROW.get(i), 160 + 122 * i, 38, 110, 80));
        for (int i = 0; i < 2; i++) cells.add(new Cell(InventorySlot.WEAPONS.get(i), 242 * i, 128, 230, 112));
        for (int i = 0; i < 4; i++) cells.add(new Cell(InventorySlot.ITEMS.get(i), 484 + 142 * i, 128, 130, 112));
        for (int i = 0; i < 5; i++) cells.add(new Cell(InventorySlot.AMMUNITION.get(i), Math.round(160 + 146.4F * i), 250, 134, 80));
        CELLS = List.copyOf(cells);
    }
    public static Cell at(double x, double y) { return CELLS.stream().filter(cell -> cell.contains(x, y)).findFirst().orElse(null); }
    private InventoryPanelLayout() {}
}
