package dev.stya.blockzone.client.editor;

import icyllis.modernui.core.Context;
import icyllis.modernui.view.View;
import icyllis.modernui.view.ViewGroup;
import icyllis.modernui.widget.BaseAdapter;
import icyllis.modernui.widget.TextView;

/** Recycled selection rows, independent of world/draft state. */
final class EditorRowAdapter extends BaseAdapter {
    private final Context context;
    EditorRowAdapter(Context context) { this.context = context; }
    java.util.List<EditorPanel.ListRow> rows = java.util.List.of();
    @Override public int getCount() { return rows.size(); }
    @Override public EditorPanel.ListRow getItem(int position) { return rows.get(position); }
    @Override public long getItemId(int position) { return position; }
    @Override public View getView(int position, View recycled, ViewGroup parent) {
        var label = recycled instanceof TextView t ? t : new TextView(context);
        var row = rows.get(position);
        label.setText(row.label()); label.setTextSize(13); label.setTextColor(0xffe7eef4);
        label.setPadding(label.dp(10), label.dp(8), label.dp(10), label.dp(8));
        EditorTheme.background(label, row.selected() ? 0xff355d46 : 0x00182028);
        return label;
    }
}
