package dev.stya.blockzone.client.hud.common;

import dev.stya.blockzone.client.inventory.InventoryPromptRenderer;
import icyllis.modernui.mc.text.TextLayoutEngine;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/** Reusable, aspect-preserving QHD canvas for passive HUD components. */
public final class HudCanvas implements AutoCloseable {
    public static final int TEXT = 0xFFF3F7F8, MUTED = 0xFFC3CED3, BORDER = 0x73666666;
    public static final int GREEN = 0xFF49C7A4, RED = 0xFFFF6D76;
    public final GuiGraphics gui;
    public final float width, height;
    public static float scale() {
        var window = Minecraft.getInstance().getWindow();
        return (float)(1.12 * Math.min(window.getWidth() / 2560f, window.getHeight() / 1440f) / window.getGuiScale());
    }
    public HudCanvas(GuiGraphics gui) {
        this.gui = gui;
        float scale = scale();
        width = gui.guiWidth() / scale; height = gui.guiHeight() / scale;
        gui.pose().pushPose(); gui.pose().scale(scale, scale, 1);
    }
    public void rect(float x, float y, float w, float h, int color) {
        if (w <= 0 || h <= 0) return;
        ((dev.stya.blockzone.client.ui.ModernUiGraphics)gui).rect(x, y, w, h, color);
    }
    public void panel(float x, float y, float w, float h) {
        rect(x, y, w, h, 0x3310181E);
        rect(x, y, w, 2, BORDER); rect(x, y + h - 2, w, 2, BORDER);
        rect(x, y, 2, h, TEXT); rect(x + w - 2, y, 2, h, TEXT);
        for (float edge : new float[]{x, x + w - 10}) {
            rect(edge, y, 10, 2, TEXT); rect(edge, y + h - 2, 10, 2, TEXT);
        }
    }
    public void progress(float x, float y, float w, float h, float fraction, int color) {
        rect(x, y, w, h, 0x6610181E);
        rect(x, y + h - 1, w, 1, BORDER);
        float value = Float.isFinite(fraction) ? Math.max(0, Math.min(1, fraction)) : 0;
        rect(x, y, w * value, h, color);
    }
    public float textWidth(Component text, float size) { return Minecraft.getInstance().font.width(text) * size / 8f; }
    public void text(Component text, float x, float y, float size, int color) {
        gui.pose().pushPose();
        try {
            gui.pose().translate(x, y, 0); gui.pose().scale(size / 8f, size / 8f, 1);
            gui.drawString(net.minecraft.client.Minecraft.getInstance().font, text, 0, 0, color, false);
        } finally { gui.pose().popPose(); }
    }
    public void text(String text, float x, float y, float size, int color) { text(Component.literal(text), x, y, size, color); }
    public void right(Component text, float right, float y, float size, int color) { text(text, right - textWidth(text, size), y, size, color); }
    public Component fit(Component text, float width, float size) {
        return Component.literal(TextLayoutEngine.getInstance().getStringSplitter()
                .plainHeadByWidth(text.getString(), Math.max(0, (int)(width * 8f / size)), net.minecraft.network.chat.Style.EMPTY));
    }
    public int key(KeyMapping key, int x, int y) {
        return key == null || key.isUnbound() ? 0 : InventoryPromptRenderer.renderKey(gui,
                Minecraft.getInstance().font, key.getKey(), x, y, 20);
    }
    public void textWithShadow(Component text, float x, float y, float size, int color) {
        gui.pose().pushPose();
        try {
            gui.pose().translate(x, y, 0); gui.pose().scale(size / 8f, size / 8f, 1);
            InventoryPromptRenderer.textWithShadow(gui, Minecraft.getInstance().font, text, color);
        } finally { gui.pose().popPose(); }
    }
    public int keyWidth(KeyMapping key) {
        return key == null || key.isUnbound() ? 0 : InventoryPromptRenderer.keyWidth(Minecraft.getInstance().font, key.getKey(), 20);
    }
    @Override public void close() { gui.flush(); gui.pose().popPose(); }
}
