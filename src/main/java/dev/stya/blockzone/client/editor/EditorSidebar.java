package dev.stya.blockzone.client.editor;

import icyllis.modernui.core.Context;
import icyllis.modernui.widget.Button;
import icyllis.modernui.widget.LinearLayout;
import icyllis.modernui.widget.ScrollView;
import icyllis.modernui.widget.TextView;

import static icyllis.modernui.view.ViewGroup.LayoutParams.*;

/** A narrow inspector with a scrollable form and a fixed status/action area. */
final class EditorSidebar {
    final LinearLayout view, body;
    final TextView status;
    final Button save;
    EditorSidebar(Context context, EditorControls controls, String title, String closeLabel,
                  String saveLabel, Runnable close, Runnable onSave) {
        view = controls.column();
        view.setPadding(view.dp(10), view.dp(8), view.dp(10), view.dp(8));
        EditorTheme.background(view, 0xe8182028);
        controls.label(view, title, 15);
        var scroll = new ScrollView(context); scroll.setFillViewport(false);
        view.addView(scroll, new LinearLayout.LayoutParams(MATCH_PARENT, 0, 1));
        body = controls.column();
        scroll.addView(body, new ScrollView.LayoutParams(MATCH_PARENT, WRAP_CONTENT));
        status = controls.label(view, "", 12); status.setTextColor(0xffb8c8d8);
        controls.divider(view);
        var footer = controls.row(view);
        controls.button(footer, closeLabel, close);
        save = onSave == null ? null : controls.button(footer, saveLabel, onSave);
        if (save != null) EditorTheme.accent(save, 0xff70b7ff);
    }
}
