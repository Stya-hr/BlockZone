package dev.stya.blockzone.client.hud.squad;

import icyllis.modernui.mc.text.TextLayoutEngine;
import net.minecraft.client.gui.GuiGraphics;

/** Modern UI text bridge in QHD design pixels; never creates an interactive screen. */
final class SquadHudGraphics {
    final GuiGraphics gui;
    final SquadVitalsAnimation animations;
    SquadHudGraphics(GuiGraphics gui, SquadVitalsAnimation animations) {
        this.gui = gui;
        this.animations = animations;
    }
    void rect(float x, float y, float width, float height, int color) {
        if (!(width > 0 && height > 0)) return;
        ((dev.stya.blockzone.client.ui.ModernUiGraphics)gui).rect(x, y, width, height, color);
    }
    void line(float x1, float y1, float x2, float y2, float thickness, int color) {
        ((dev.stya.blockzone.client.ui.ModernUiGraphics)gui).line(x1, y1, x2, y2, thickness, color);
    }
    void centeredText(String text, float centerX, float y, float size, int color) {
        float width = net.minecraft.client.Minecraft.getInstance().font.width(text) * size / 8f;
        text(text, centerX - width / 2, y, size, color);
    }
    void text(String text, float x, float y, float size, int color) {
        gui.pose().pushPose();
        try {
            gui.pose().translate(x, y, 0);
            float scale = size / 8f;
            gui.pose().scale(scale, scale, 1);
            gui.drawString(net.minecraft.client.Minecraft.getInstance().font, text, 0, 0, color, false);
        } finally { gui.pose().popPose(); }
    }
    void name(String text, float x, float y, float size, float width, int color) {
        String fitted = fit(text, width, size);
        // SVG names have a 1px green outline, independent of font size.
        for (int dy = -1; dy <= 1; dy++) for (int dx = -1; dx <= 1; dx++)
            if (dx != 0 || dy != 0) text(fitted, x + dx, y + dy, size, SquadHudStyle.NAME_OUTLINE);
        text(fitted, x, y, size, color);
    }
    String fit(String text, float width, float size) {
        var splitter = TextLayoutEngine.getInstance().getStringSplitter();
        return splitter.plainHeadByWidth(text, Math.max(0, (int)(width * 8f / size)), net.minecraft.network.chat.Style.EMPTY);
    }
}
