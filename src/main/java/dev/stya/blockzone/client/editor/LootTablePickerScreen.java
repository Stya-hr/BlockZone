package dev.stya.blockzone.client.editor;

import icyllis.modernui.widget.LinearLayout;
import net.minecraft.network.chat.Component;
import java.util.List;
import java.util.Locale;

final class LootTablePickerScreen extends ModernEditorScreen {
    private final LootCrateEditorScreen parent;
    private final List<String> tables;
    private volatile String query = "";
    private LinearLayout results;

    LootTablePickerScreen(LootCrateEditorScreen parent, List<String> tables) {
        super(Component.translatable("editor.blockzone.loot.choose_title"));
        this.parent = parent; this.tables = List.copyOf(tables);
    }
    @Override boolean hasSaveAction() { return false; }
    @Override void buildPanel(EditorPanel ui) {
        ui.field(ui.row(ui.body()), EditorPanel.tr("editor.blockzone.ui.search_tables"), query, 256, value -> {
            query = value;
            var root = ui.root();
            if (root != null) root.post(() -> fillResults(ui));
        });
        results = ui.column();
        ui.body().addView(results, new LinearLayout.LayoutParams(-1, -2));
        fillResults(ui);
    }
    private void fillResults(EditorPanel ui) {
        if (results == null || ui.root() == null) return;
        results.removeAllViews();
        String filter = query.strip().toLowerCase(Locale.ROOT);
        var visible = tables.stream().filter(id -> id.toLowerCase(Locale.ROOT).contains(filter)).toList();
        if (visible.isEmpty()) ui.label(results, EditorPanel.tr("editor.blockzone.ui.no_results"), 12);
        for (String id : visible) ui.button(ui.row(results), id, () -> parent.pick(id));
    }
    @Override public void onClose() { minecraft.setScreen(parent); }
}
