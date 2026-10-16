package dev.stya.blockzone.client.hud.weapon;

import dev.stya.blockzone.client.inventory.GunHudIcon;
import dev.stya.blockzone.client.inventory.InventoryPromptRenderer;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** QHD design pixels, with Modern UI text and shared inventory gun/key renderers. */
final class WeaponHudGraphics {
    static final int WHITE = 0xFFF3F7F8, DIM = 0x73666666, RED = 0xFFFF6D76;
    final GuiGraphics gui;
    WeaponHudGraphics(GuiGraphics gui) { this.gui = gui; }
    void rect(float x, float y, float w, float h, int color) {
        ((dev.stya.blockzone.client.ui.ModernUiGraphics)gui).rect(x, y, w, h, color);
    }
    void box(int x, int y, int w, int h, boolean selected, boolean weapon) {
        rect(x, y, w, h, dev.stya.blockzone.client.hud.common.HudCanvas.PANEL_BACKGROUND);
        if (weapon && selected) {
            // SVG: gray horizontal edges with white 10px brackets at either end.
            rect(x, y, w, 2, DIM); rect(x, y + h - 2, w, 2, DIM);
            rect(x, y, 3, h, WHITE); rect(x + w - 3, y, 3, h, WHITE);
            rect(x, y, 10, 3, WHITE); rect(x + w - 10, y, 10, 3, WHITE);
            rect(x, y + h - 3, 10, 3, WHITE); rect(x + w - 10, y + h - 3, 10, 3, WHITE);
            return;
        }
        int t = selected ? 3 : 2, color = selected ? WHITE : DIM;
        rect(x, y, w, t, color); rect(x, y + h - t, w, t, color);
        rect(x, y + t, t, h - 2 * t, color); rect(x + w - t, y + t, t, h - 2 * t, color);
    }
    float width(String text, float size) { return width(Component.literal(text), size); }
    float width(Component text, float size) { return Minecraft.getInstance().font.width(text) * size / 8f; }
    void text(String text, float x, float y, float size, int color) {
        text(Component.literal(text), x, y, size, color);
    }
    void text(Component text, float x, float y, float size, int color) {
        gui.pose().pushPose();
        gui.pose().translate(x, y, 0); gui.pose().scale(size / 8f, size / 8f, 1);
        gui.drawString(net.minecraft.client.Minecraft.getInstance().font, text, 0, 0, color, false);
        gui.pose().popPose();
    }
    void key(KeyMapping mapping, int x, int y) {
        if (!mapping.isUnbound()) InventoryPromptRenderer.renderKey(gui, Minecraft.getInstance().font, mapping.getKey(), x, y, 20);
    }
    void item(ItemStack stack, int x, int y, int w, int h, boolean weapon) {
        if (stack.isEmpty()) return;
        if (weapon && GunHudIcon.draw(gui, stack, x, y, w, h)) return;
        gui.flush();
        int size = Math.min(w, h);
        gui.pose().pushPose();
        gui.pose().translate(x + (w - size) / 2f, y + (h - size) / 2f, 0);
        gui.pose().scale(size / 16f, size / 16f, 1);
        gui.renderItem(stack, 0, 0);
        gui.flush(); gui.pose().popPose();
    }
}
