package dev.stya.blockzone.client.hud.squad;

import icyllis.modernui.mc.text.TextLayoutEngine;
import net.minecraft.client.gui.Font;
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
        // Keep fractional SVG dimensions (including 114.667px armor segments).
        var matrix = gui.pose().last().pose();
        var vertices = gui.bufferSource().getBuffer(net.minecraft.client.renderer.RenderType.gui());
        vertices.vertex(matrix, x, y + height, 0).color(color).endVertex();
        vertices.vertex(matrix, x + width, y + height, 0).color(color).endVertex();
        vertices.vertex(matrix, x + width, y, 0).color(color).endVertex();
        vertices.vertex(matrix, x, y, 0).color(color).endVertex();
    }
    void line(float x1, float y1, float x2, float y2, float thickness, int color) {
        float length = (float)Math.hypot(x2 - x1, y2 - y1);
        if (length == 0) return;
        float dx = -(y2 - y1) * thickness / (2 * length);
        float dy = (x2 - x1) * thickness / (2 * length);
        var matrix = gui.pose().last().pose();
        var vertices = gui.bufferSource().getBuffer(net.minecraft.client.renderer.RenderType.gui());
        vertices.vertex(matrix, x1 + dx, y1 + dy, 0).color(color).endVertex();
        vertices.vertex(matrix, x2 + dx, y2 + dy, 0).color(color).endVertex();
        vertices.vertex(matrix, x2 - dx, y2 - dy, 0).color(color).endVertex();
        vertices.vertex(matrix, x1 - dx, y1 - dy, 0).color(color).endVertex();
    }
    void centeredText(String text, float centerX, float y, float size, int color) {
        float width = net.minecraft.client.Minecraft.getInstance().font.width(text) * size / 8f;
        text(text, centerX - width / 2, y, size, color);
    }
    void text(String text, float x, float y, float size, int color) {
        var engine = TextLayoutEngine.getInstance();
        gui.pose().pushPose();
        try {
            gui.pose().translate(x, y, 0);
            float scale = size / 8f;
            gui.pose().scale(scale, scale, 1);
            engine.getTextRenderer().drawText(text, 0, 0, color, false,
                    gui.pose().last().pose(), gui.bufferSource(), Font.DisplayMode.NORMAL, 0, 0xF000F0);
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
