package dev.stya.blockzone.client.editor;

import icyllis.modernui.text.TextUtils;
import icyllis.modernui.view.Gravity;
import icyllis.modernui.view.View;
import icyllis.modernui.widget.FrameLayout;
import icyllis.modernui.widget.LinearLayout;
import icyllis.modernui.widget.TextView;

import static icyllis.modernui.view.ViewGroup.LayoutParams.WRAP_CONTENT;

/** Two fixed status rows, outside the inspector. Long messages remain available on hover. */
final class EditorStatusBar {
    final LinearLayout view;
    private final TextView summary, feedback;
    private String lastValue;
    EditorStatusBar(EditorControls controls, FrameLayout root) {
        view = controls.column();
        view.setPadding(view.dp(10), view.dp(4), view.dp(10), view.dp(4));
        EditorTheme.background(view, 0xe8182028);
        summary = line(controls);
        feedback = line(controls);
        var params = new FrameLayout.LayoutParams(1, WRAP_CONTENT, Gravity.LEFT | Gravity.BOTTOM);
        int gap = view.dp(8);
        params.setMargins(gap, gap, gap, gap);
        root.addView(view, params);
        view.setVisibility(View.GONE);
    }
    private TextView line(EditorControls controls) {
        var text = controls.label(view, "", 12);
        text.setTextColor(0xffb8c8d8);
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
        String[] lines = value.split("\\n", 2);
        summary.setText(lines[0]);
        feedback.setText(lines.length == 2 ? lines[1].replace('\n', ' ') : "");
        view.setTooltipText(value.isEmpty() ? null : value);
        view.setVisibility(value.isEmpty() ? View.GONE : View.VISIBLE);
    }
    int listBottomMargin() {
        return view.getVisibility() == View.GONE ? view.dp(8) : Math.max(view.getHeight(), view.dp(52)) + view.dp(16);
    }
}
