package dev.stya.blockzone.client.editor;

import icyllis.modernui.graphics.drawable.GradientDrawable;
import icyllis.modernui.view.View;
import icyllis.modernui.widget.Button;

/** Shared editor palette, shapes and enabled/hover/pressed/focus states. */
final class EditorTheme {
    private EditorTheme() {}
    static void accent(Button button, int color) {
        button.setTextColor(new icyllis.modernui.util.ColorStateList(
                new int[][]{{-icyllis.modernui.R.attr.state_enabled}, {}},
                new int[]{0xff687581, color}));
    }
    static void styleButton(Button button) {
        int enabled = icyllis.modernui.R.attr.state_enabled;
        int pressed = icyllis.modernui.R.attr.state_pressed;
        int hovered = icyllis.modernui.R.attr.state_hovered;
        int focused = icyllis.modernui.R.attr.state_focused;
        var states = new int[][]{{-enabled}, {pressed}, {focused}, {hovered}, {}};
        var background = new GradientDrawable();
        background.setCornerRadius(button.dp(5));
        background.setColor(new icyllis.modernui.util.ColorStateList(states,
                new int[]{0xff29323c, 0xff344453, 0xff4b6074, 0xff4a5a6b, 0xff3b4958}));
        background.setStroke(button.dp(1), new icyllis.modernui.util.ColorStateList(states,
                new int[]{0xff1b232c, 0xff111923, 0xff83b8e7, 0xff17222d, 0xff17222d}));
        button.setBackground(background);
        button.setPadding(button.dp(8), button.dp(5), button.dp(8), button.dp(5));
    }
    static void background(View view, int color) {
        var bg = new GradientDrawable(); bg.setColor(color); bg.setCornerRadius(view.dp(6)); view.setBackground(bg);
    }
}
