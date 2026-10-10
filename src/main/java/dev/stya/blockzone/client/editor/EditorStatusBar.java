package dev.stya.blockzone.client.editor;

import icyllis.modernui.text.TextUtils;
import icyllis.modernui.view.Gravity;
import icyllis.modernui.view.View;
import icyllis.modernui.widget.FrameLayout;
import icyllis.modernui.widget.LinearLayout;
import icyllis.modernui.widget.TextView;

import static icyllis.modernui.view.ViewGroup.LayoutParams.WRAP_CONTENT;

/** Compact, background-free selection summary outside the inspector. */
final class EditorStatusBar {
    final LinearLayout view;
    private final TextView summary;
    private String lastValue;
    EditorStatusBar(EditorControls controls, FrameLayout root) {
        view = controls.column();
        summary = line(controls);
        var params = new FrameLayout.LayoutParams(1, WRAP_CONTENT, Gravity.LEFT | Gravity.BOTTOM);
        int gap = view.dp(8);
        params.setMargins(gap, gap, gap, gap);
        root.addView(view, params);
        view.setVisibility(View.GONE);
    }
    private TextView line(EditorControls controls) {
        var text = controls.label(view, "", 12);
        text.setTextColor(0xffe7eef4);
        text.setSingleLine();
        text.setEllipsize(TextUtils.TruncateAt.END);
        return text;
    }
    void resize(int viewportWidth) {
        var params = (FrameLayout.LayoutParams) view.getLayoutParams();
        int width = Math.max(1, Math.min(view.dp(520), viewportWidth - view.dp(16)));
        if (params.width != width) { params.width = width; view.setLayoutParams(params); }
    }
    void show(String value) {
        if (value.equals(lastValue)) return;
        lastValue = value;
        summary.setText(value);
        view.setVisibility(value.isEmpty() ? View.GONE : View.VISIBLE);
    }
    int listBottomMargin() {
        return view.getVisibility() == View.GONE ? view.dp(8) : Math.max(view.getHeight(), view.dp(24)) + view.dp(16);
    }
}
