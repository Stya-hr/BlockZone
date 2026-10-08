package dev.stya.blockzone.client.editor;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/** Shared, texture-free controls for the in-world editors. */
final class EditorUi {
    static final int TEXT = 0xffe5edf3;
    static final int MUTED = 0xff9aabba;
    static final int ACCENT = 0xff64d8c0;
    static final int WARNING = 0xffefbe76;
    static final int DANGER = 0xffff929c;
    static final int BORDER = 0xff344653;
    static final int PANEL = 0xf0111b25;
    static final int SURFACE = 0xe81b2935;
    static final int HOVER = 0xff2b4150;

    enum Tone { DEFAULT, PRIMARY, DANGER }

    private EditorUi() {}

    static int sidebarWidth(int guiWidth) {
        return Math.min(Math.max(144, guiWidth - 132), Mth.clamp(guiWidth / 3 + 20, 188, 244));
    }

    static Button button(int x, int y, int width, int height, Component label, Tone tone, Runnable action) {
        var button = new EditorButton(x, y, width, height, label, tone, action);
        button.setTooltip(Tooltip.create(label));
        return button;
    }

    static EditBox input(Font font, int x, int y, int width, int height, Component label) {
        var field = new EditorTextField(font, x, y, width, height, label);
        field.setTextColor(TEXT);
        field.setTextColorUneditable(MUTED);
        return field;
    }

    static void invalid(EditBox field, boolean invalid) {
        if (field instanceof EditorTextField themed) themed.invalid = invalid;
        field.setTextColor(invalid ? DANGER : TEXT);
    }

    static Checkbox toggle(int x, int y, int width, Component label, boolean selected) {
        var checkbox = new EditorCheckbox(x, y, width, label, selected);
        checkbox.setTooltip(Tooltip.create(label));
        return checkbox;
    }

    static void panel(GuiGraphics graphics, int x, int y, int width, int height) {
        if (width <= 0 || height <= 0) return;
        graphics.fill(x + 2, y + 3, x + width + 2, y + height + 3, 0x38000000);
        graphics.fill(x, y, x + width, y + height, PANEL);
        graphics.renderOutline(x, y, width, height, BORDER);
        graphics.fill(x + 1, y + 1, x + width - 1, y + 2, 0xff425b66);
    }

    static void card(GuiGraphics graphics, int x, int y, int width, int height) {
        if (width <= 0 || height <= 0) return;
        graphics.fill(x, y, x + width, y + height, SURFACE);
        graphics.renderOutline(x, y, width, height, BORDER);
    }

    static void text(GuiGraphics graphics, Font font, Component text, int x, int y, int maxWidth, int color) {
        if (maxWidth <= 0) return;
        String value = text.getString();
        if (font.width(value) > maxWidth) {
            String suffix = "...";
            if (font.width(suffix) > maxWidth) return;
            value = font.plainSubstrByWidth(value, maxWidth - font.width(suffix)) + suffix;
        }
        graphics.drawString(font, value, x, y, color, false);
    }

    static void section(GuiGraphics graphics, Font font, Component text, int x, int y, int width) {
        text(graphics, font, text, x, y, width, MUTED);
        int lineX = x + font.width(text) + 9;
        if (lineX < x + width) graphics.fill(lineX, y + 4, x + width, y + 5, BORDER);
    }

    static void scrollbar(GuiGraphics graphics, int x, int y, int height, int contentHeight, int scroll) {
        if (height <= 0 || contentHeight <= height) return;
        graphics.fill(x, y, x + 2, y + height, BORDER);
        int thumb = Math.min(height, Math.max(12, height * height / contentHeight));
        int offset = (height - thumb) * Mth.clamp(scroll, 0, contentHeight - height) / (contentHeight - height);
        graphics.fill(x, y + offset, x + 2, y + offset + thumb, ACCENT);
    }

    private static final class EditorButton extends Button {
        private final Tone tone;

        private EditorButton(int x, int y, int width, int height, Component label, Tone tone, Runnable action) {
            super(x, y, width, height, label, button -> action.run(), DEFAULT_NARRATION);
            this.tone = tone;
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            boolean hovered = isHoveredOrFocused();
            int fill = !active ? 0xff19232d : switch (tone) {
                case PRIMARY -> hovered ? 0xff85e5ce : ACCENT;
                case DANGER -> hovered ? 0xff51333d : 0xff32252e;
                case DEFAULT -> hovered ? HOVER : 0xff23333f;
            };
            int border = !active ? 0xff293743 : hovered ? ACCENT : tone == Tone.DANGER ? 0xff66404c : BORDER;
            graphics.fill(getX(), getY(), getX() + width, getY() + height, fill);
            graphics.renderOutline(getX(), getY(), width, height, border);
            if (isFocused()) graphics.renderOutline(getX() + 2, getY() + 2, width - 4, height - 4, TEXT);
            Font font = Minecraft.getInstance().font;
            String label = getMessage().getString();
            if (font.width(label) > width - 12) label = font.plainSubstrByWidth(label, Math.max(0, width - 12 - font.width("..."))) + "...";
            int color = !active ? 0xff71818e : tone == Tone.PRIMARY ? 0xff102c29 : tone == Tone.DANGER ? DANGER : TEXT;
            graphics.drawString(font, label, getX() + (width - font.width(label)) / 2, getY() + (height - 8) / 2, color, false);
        }
    }

    private static final class EditorTextField extends EditBox {
        private boolean invalid;

        private EditorTextField(Font font, int x, int y, int width, int height, Component label) {
            super(font, x, y, width, height, label);
        }

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            super.renderWidget(graphics, mouseX, mouseY, partialTick);
            // Retain vanilla caret, selection and hit testing; replace only the frame.
            int border = invalid ? DANGER : isFocused() ? ACCENT : isHovered() && active ? MUTED : BORDER;
            graphics.renderOutline(getX() - 1, getY() - 1, width + 2, height + 2, border);
        }
    }

    private static final class EditorCheckbox extends Checkbox {
        private EditorCheckbox(int x, int y, int width, Component label, boolean selected) {
            super(x, y, width, 22, label, selected);
        }

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            int x = getX(), y = getY();
            graphics.fill(x, y, x + width, y + height, isHoveredOrFocused() && active ? HOVER : SURFACE);
            if (isFocused()) graphics.renderOutline(x, y, width, height, ACCENT);
            graphics.fill(x + 5, y + 5, x + 17, y + 17, selected() ? active ? ACCENT : BORDER : 0xff101922);
            graphics.renderOutline(x + 5, y + 5, 12, 12, active && isHoveredOrFocused() ? ACCENT : BORDER);
            if (selected()) {
                graphics.fill(x + 8, y + 10, x + 10, y + 13, 0xff12352e);
                graphics.fill(x + 10, y + 11, x + 12, y + 14, 0xff12352e);
                graphics.fill(x + 12, y + 8, x + 14, y + 12, 0xff12352e);
            }
            text(graphics, Minecraft.getInstance().font, getMessage(), x + 24, y + 7, width - 30, active ? TEXT : MUTED);
        }
    }
}
