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
    private List<String> visible = List.of();
    private final java.util.List<icyllis.modernui.widget.Button> choices = new java.util.ArrayList<>();
    private int highlighted;

    LootTablePickerScreen(LootCrateEditorScreen parent, List<String> tables) {
        super(Component.translatable("editor.blockzone.loot.choose_title"));
        this.parent = parent; this.tables = List.copyOf(tables);
    }
    @Override boolean hasSaveAction() { return false; }
    @Override void buildPanel(EditorPanel ui) {
        var search = ui.field(ui.row(ui.body()), EditorPanel.tr("editor.blockzone.ui.search_tables"), query, 256, value -> {
            query = value;
            var root = ui.root();
            if (root != null) root.post(() -> fillResults(ui));
        });
        search.setOnKeyListener((v, key, event) -> {
            boolean arrow = key == icyllis.modernui.view.KeyEvent.KEY_UP || key == icyllis.modernui.view.KeyEvent.KEY_DOWN;
            boolean enter = key == icyllis.modernui.view.KeyEvent.KEY_ENTER || key == icyllis.modernui.view.KeyEvent.KEY_KP_ENTER;
            if ((!arrow && !enter) || visible.isEmpty()) return false;
            if (event.getAction() == icyllis.modernui.view.KeyEvent.ACTION_DOWN) {
                if (arrow) {
                    highlighted = Math.max(0, Math.min(visible.size() - 1, highlighted + (key == icyllis.modernui.view.KeyEvent.KEY_DOWN ? 1 : -1)));
                    highlight();
                    var button = choices.get(highlighted);
                    button.requestRectangleOnScreen(new icyllis.modernui.graphics.Rect(0, 0, button.getWidth(), button.getHeight()));
                } else {
                    String id = visible.get(highlighted);
                    ui.action(() -> parent.pick(id));
                }
            }
            return true;
        });
        results = ui.column();
        ui.body().addView(results, new LinearLayout.LayoutParams(-1, -2));
        fillResults(ui);
    }
    private void fillResults(EditorPanel ui) {
        if (results == null || ui.root() == null) return;
        results.removeAllViews();
        String filter = query.strip().toLowerCase(Locale.ROOT);
        var matches = tables.stream().filter(id -> id.toLowerCase(Locale.ROOT).contains(filter)).toList();
        if (!matches.equals(visible)) highlighted = 0;
        visible = matches;
        choices.clear();
        if (visible.isEmpty()) ui.label(results, EditorPanel.tr("editor.blockzone.ui.no_results"), 12);
        for (String id : visible) choices.add(ui.button(ui.row(results), id, () -> parent.pick(id)));
        highlight();
    }
    private void highlight() {
        for (int i = 0; i < choices.size(); i++) choices.get(i).setSelected(i == highlighted);
    }
    @Override public void onClose() { minecraft.setScreen(parent); }
}
