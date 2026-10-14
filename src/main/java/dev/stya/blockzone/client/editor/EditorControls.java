package dev.stya.blockzone.client.editor;

import icyllis.modernui.core.Context;
import icyllis.modernui.view.View;
import icyllis.modernui.widget.Button;
import icyllis.modernui.widget.LinearLayout;
import icyllis.modernui.widget.TextView;
import java.util.function.Consumer;

import static icyllis.modernui.view.ViewGroup.LayoutParams.*;

/** Stateless widget factory. Callbacks use the owning panel's game-thread dispatcher. */
final class EditorControls {
    private final Context context;
    private final Consumer<Runnable> dispatch;
    EditorControls(Context context, Consumer<Runnable> dispatch) {
        this.context = context; this.dispatch = dispatch;
    }
    private void action(Runnable callback) { dispatch.accept(callback); }
    LinearLayout column() { var v = new LinearLayout(context); v.setOrientation(LinearLayout.VERTICAL); return v; }
    LinearLayout row(LinearLayout parent) {
        var row = new LinearLayout(context); row.setOrientation(LinearLayout.HORIZONTAL);
        parent.addView(row, new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)); return row;
    }
    void divider(LinearLayout parent) {
        var line = new View(context); EditorTheme.background(line, 0xff34414e);
        var params = new LinearLayout.LayoutParams(MATCH_PARENT, line.dp(1));
        params.setMargins(0, line.dp(6), 0, line.dp(6)); parent.addView(line, params);
    }
    void section(LinearLayout body, String caption) {
        divider(body);
        var title = label(body, caption, 12);
        title.setTextColor(0xff83b8e7);
    }
    Button danger(Button button) { EditorTheme.accent(button, 0xffff9b93); return button; }
    TextView label(LinearLayout parent, String value, int size) {
        var label = new TextView(context); label.setText(value); label.setTextSize(size); label.setTextColor(0xffe7eef4);
        label.setPadding(0, label.dp(4), 0, label.dp(4));
        parent.addView(label, new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)); return label;
    }
    Button button(LinearLayout parent, String value, Runnable handler) {
        var button = new Button(context); button.setText(value); button.setTextSize(12);
        button.setMinHeight(button.dp(30)); button.setMinimumWidth(0);
        EditorTheme.styleButton(button);
        button.setOnClickListener(v -> action(handler));
        var params = new LinearLayout.LayoutParams(0, WRAP_CONTENT, 1); params.setMargins(0, 0, button.dp(3), button.dp(3));
        parent.addView(button, params); return button;
    }
}
