package dev.stya.blockzone.util.editor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class LootCrateDraft {
    private final Map<LootCrateEdit.Position, LootCrateEdit> original = new LinkedHashMap<>();
    private final Map<LootCrateEdit.Position, LootCrateEdit> entries = new LinkedHashMap<>();
    private final Set<LootCrateEdit.Position> selected = new HashSet<>();
    private String filter = "";

    public LootCrateDraft(List<LootCrateEdit> crates) {
        if (crates.size() > LootCrateEdit.MAX_CRATES) throw new IllegalArgumentException("Too many crates");
        for (var crate : crates) {
            if (original.put(crate.position(), crate) != null) throw new IllegalArgumentException("Duplicate crate position");
        }
        entries.putAll(original);
    }

    public List<LootCrateEdit> entries() { return List.copyOf(entries.values()); }
    public List<LootCrateEdit> visible() {
        return entries.values().stream().filter(this::visible).toList();
    }
    private boolean visible(LootCrateEdit entry) {
        return filter.isBlank() || (entry.x() + " " + entry.y() + " " + entry.z() + " " + entry.table())
                .toLowerCase(Locale.ROOT).contains(filter);
    }
    public void filter(String value) {
        filter = value.strip().toLowerCase(Locale.ROOT);
        // Never apply to hidden selections left over from a different search.
        selected.clear();
    }
    public boolean selected(LootCrateEdit entry) { return selected.contains(entry.position()); }
    public int selectedCount() { return selected.size(); }
    public void toggle(LootCrateEdit entry) {
        if (!entries.containsKey(entry.position())) return;
        if (!selected.remove(entry.position())) selected.add(entry.position());
    }
    public void clearSelection() { selected.clear(); }
    public void selectAll() {
        selected.clear();
        visible().forEach(entry -> selected.add(entry.position()));
    }
    public void selectBox(double x1, double z1, double x2, double z2, boolean add) {
        if (!add) selected.clear();
        for (var entry : visible()) {
            if (entry.x() >= Math.min(x1, x2) && entry.x() <= Math.max(x1, x2)
                    && entry.z() >= Math.min(z1, z2) && entry.z() <= Math.max(z1, z2)) {
                selected.add(entry.position());
            }
        }
    }
    public void selectPositions(java.util.Collection<LootCrateEdit.Position> positions, boolean add) {
        if (!add) selected.clear();
        for (var entry : visible()) if (positions.contains(entry.position())) selected.add(entry.position());
    }
    public void apply(String table, long seed, boolean close) {
        String normalized = table.strip();
        if (!normalized.contains(":")) normalized = "minecraft:" + normalized;
        if (normalized.length() > 256 || !normalized.matches("[a-z0-9_.-]+:[a-z0-9/._-]+"))
            throw new IllegalArgumentException("Invalid loot table ID");
        if (selected.isEmpty()) throw new IllegalArgumentException("Select crates first");
        for (var pos : selected) {
            var entry = entries.get(pos);
            entries.put(pos, new LootCrateEdit(entry.x(), entry.y(), entry.z(), normalized, seed, close ? false : entry.opened()));
        }
    }
    public List<LootCrateEdit> changes() {
        var result = new ArrayList<LootCrateEdit>();
        entries.forEach((pos, entry) -> { if (!entry.equals(original.get(pos))) result.add(entry); });
        return List.copyOf(result);
    }
    public boolean dirty() { return !changes().isEmpty(); }
    public void markSaved() { original.clear(); original.putAll(entries); }
}
