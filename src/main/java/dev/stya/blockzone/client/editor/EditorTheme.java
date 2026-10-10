package dev.stya.blockzone.client.editor;

import icyllis.modernui.graphics.drawable.GradientDrawable;
import icyllis.modernui.view.View;
import icyllis.modernui.widget.Button;
import icyllis.modernui.widget.EditText;
import icyllis.modernui.util.ColorStateList;

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
    static void styleInput(EditText input) {
        int enabled = icyllis.modernui.R.attr.state_enabled;
        int focused = icyllis.modernui.R.attr.state_focused;
        int hovered = icyllis.modernui.R.attr.state_hovered;
        var states = new int[][]{{-enabled}, {focused}, {hovered}, {}};
        var background = new GradientDrawable();
        background.setCornerRadius(input.dp(6));
        background.setColor(new ColorStateList(states,
                new int[]{0xff222a32, 0xff141e28, 0xff18222c, 0xff141c24}));
        background.setStroke(input.dp(1), new ColorStateList(states,
                new int[]{0xff35414d, 0xff4096ff, 0xff69b1ff, 0xff526171}));
        input.setBackground(background);
        input.setTextColor(new ColorStateList(new int[][]{{-enabled}, {}},
                new int[]{0xff74818d, 0xffe7eef4}));
        input.setTextSize(12);
        input.setMinHeight(input.dp(26));
        input.setPadding(input.dp(7), input.dp(3), input.dp(7), input.dp(3));
    }
    static void background(View view, int color) {
        var bg = new GradientDrawable(); bg.setColor(color); bg.setCornerRadius(view.dp(6)); view.setBackground(bg);
    }
}
