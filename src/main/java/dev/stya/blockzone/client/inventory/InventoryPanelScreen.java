package dev.stya.blockzone.client.inventory;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.stya.blockzone.inventory.InventorySlot;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/** Read-only inventory panel; server snapshots own reserves and vanilla owns equipped weapons. */
public final class InventoryPanelScreen extends Screen {
    private static final float PANEL_SCALE = 1.2F;
    private static final int BOTTOM_MARGIN = 122;
    private static final int FOREGROUND = 0xFFF3F7F8;
    private static final float EMPTY_ICON_ALPHA = .25F;
    private static final int LABEL_HEIGHT = 26;
    private static final int BORDER = 0x73666666;
    private static final List<InventorySlot> EQUIPMENT = InventorySlot.EQUIPMENT;

    public InventoryPanelScreen() {
        super(Component.translatable("screen.blockzone.inventory_preview"));
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        // Work in the export's 2560 x 1440 coordinates, anchored to the bottom center.
        float scale = panelScale();
        gui.pose().pushPose();
        gui.pose().translate((width - 1040 * scale) / 2F, height - (410 + BOTTOM_MARGIN) * scale, 0);
        gui.pose().scale(scale, scale, 1);
        slot(gui, 920, 932, 720, 28, false);
        for (int i = 0; i < EQUIPMENT.size(); i++) {
            int x = 920 + 122 * i;
            slot(gui, x, 970, 110, 80, false);
            if (stack(EQUIPMENT.get(i)).isEmpty()) icon(gui, EQUIPMENT.get(i).icon(), x + 35, 990, 40, 40, EMPTY_ICON_ALPHA);
        }
        for (int i = 0; i < InventorySlot.WEAPONS.size(); i++) {
            int x = 760 + 242 * i;
            slot(gui, x, 1060, 230, 112, minecraft.player != null && minecraft.player.getInventory().selected == i && !stack(InventorySlot.WEAPONS.get(i)).isEmpty());
            if (stack(InventorySlot.WEAPONS.get(i)).isEmpty()) icon(gui, "rifle", x + 37, 1075, 156, 56, EMPTY_ICON_ALPHA);
        }
        var items = InventorySlot.ITEMS;
        for (int i = 0; i < items.size(); i++) {
            int x = 1244 + 142 * i;
            slot(gui, x, 1060, 130, 112, false);
            if (stack(items.get(i)).isEmpty()) {
                if (i == 0) icon(gui, items.get(i).icon(), x + 53, 1079, 24, 48, EMPTY_ICON_ALPHA);
                else icon(gui, items.get(i).icon(), x + 45, 1086, 40, 42, EMPTY_ICON_ALPHA);
            }
        }
        for (int i = 0; i < InventorySlot.AMMUNITION.size(); i++) {
            int x = Math.round(920 + 146.4F * i);
            slot(gui, x, 1182, 134, 80, false);
            if (stack(InventorySlot.AMMUNITION.get(i)).isEmpty()) icon(gui, "ammunition", x + 47, 1187, 40, 40, EMPTY_ICON_ALPHA);
        }
        // Text strips sit above the empty-slot silhouettes and inside the border.
        for (int i = 0; i < 2; i++) labelBackground(gui, 760 + 242 * i, 1060, 230, 112);
        for (int i = 0; i < 4; i++) labelBackground(gui, 1244 + 142 * i, 1060, 130, 112);
        for (int i = 0; i < 5; i++) labelBackground(gui, Math.round(920 + 146.4F * i), 1182, 134, 80);
        text(gui, Component.translatable("screen.blockzone.inventory_preview").getString(), 930, 936, 1.5F);
        text(gui, Component.translatable("inventory.blockzone.close").getString(), 1210, 1322, 1.5F);
        for (int i = 0; i < 2; i++) label(gui, InventorySlot.WEAPONS.get(i), 760 + 242 * i, 1060, 230, 112);
        for (int i = 0; i < 4; i++) label(gui, items.get(i), 1244 + 142 * i, 1060, 130, 112);
        for (int i = 0; i < 5; i++) label(gui, InventorySlot.AMMUNITION.get(i), Math.round(920 + 146.4F * i), 1182, 134, 80);
        for (int i = 0; i < EQUIPMENT.size(); i++) item(gui, EQUIPMENT.get(i), 920 + 122 * i + 31, 986);
        for (int i = 0; i < InventorySlot.WEAPONS.size(); i++) item(gui, InventorySlot.WEAPONS.get(i), 760 + 242 * i + 91, 1076);
        for (int i = 0; i < items.size(); i++) item(gui, items.get(i), 1244 + 142 * i + 41, 1076);
        for (int i = 0; i < InventorySlot.AMMUNITION.size(); i++) item(gui, InventorySlot.AMMUNITION.get(i), Math.round(920 + 146.4F * i) + 43, 1188);
        gui.pose().popPose();
    }

    private void label(GuiGraphics gui, InventorySlot slot, int x, int y, int w, int h) {
        var item = stack(slot);
        String name = item.isEmpty() ? Component.translatable("slot.blockzone." + slot.id()).getString() : item.getHoverName().getString();
        text(gui, font.plainSubstrByWidth(name, (int) ((w - 12) / 1.8F)), x + 6, y + h - 21, 1.8F);
    }

    private void text(GuiGraphics gui, String value, int x, int y, float scale) {
        gui.pose().pushPose();
        gui.pose().translate(x - 760, y - 932, 0);
        gui.pose().scale(scale, scale, 1);
        gui.drawString(font, value, 0, 0, FOREGROUND, false);
        gui.pose().popPose();
    }

    private net.minecraft.world.item.ItemStack stack(InventorySlot slot) {
        if (minecraft.player == null) return net.minecraft.world.item.ItemStack.EMPTY;
        return dev.stya.blockzone.inventory.PlayerPanelInventory.get(minecraft.player)
                .filter(dev.stya.blockzone.inventory.PlayerPanelInventory::active)
                .map(data -> data.getStack(slot)).orElse(net.minecraft.world.item.ItemStack.EMPTY);
    }

    private void item(GuiGraphics gui, InventorySlot slot, int x, int y) {
        var stack = stack(slot);
        if (stack.isEmpty()) return;
        gui.pose().pushPose();
        gui.pose().translate(x - 760, y - 932, 0);
        gui.pose().scale(3, 3, 1);
        gui.renderItem(stack, 0, 0);
        gui.renderItemDecorations(font, stack, 0, 0);
        gui.pose().popPose();
    }

    private float panelScale() {
        return PANEL_SCALE * Math.min(width / 2560F, height / 1440F);
    }

    private void slot(GuiGraphics gui, int x, int y, int w, int h, boolean selected) {
        x -= 760;
        y -= 932;
        gui.fill(x, y, x + w, y + h, 0x8C000000);
        float scale = panelScale();
        float physicalScale = scale * (float) minecraft.getWindow().getGuiScale();
        float stroke = Math.max(2F, 1.5F / physicalScale);
        gui.pose().pushPose();
        gui.pose().translate(x, y, 0);
        gui.pose().scale(stroke, stroke, 1);
        int edgeWidth = Math.round(w / stroke);
        int edgeHeight = Math.round(h / stroke);
        int color = selected ? FOREGROUND : BORDER;
        gui.fill(0, 0, edgeWidth, 1, color);
        gui.fill(0, edgeHeight - 1, edgeWidth, edgeHeight, color);
        gui.fill(0, 0, 1, edgeHeight, color);
        gui.fill(edgeWidth - 1, 0, edgeWidth, edgeHeight, color);
        gui.pose().popPose();
    }

    private void labelBackground(GuiGraphics gui, int x, int y, int w, int h) {
        // 18-unit lettering plus four units of vertical padding on either side.
        int inset = Math.max(2, (int) Math.ceil(1.5F /
                (panelScale() * (float) minecraft.getWindow().getGuiScale())));
        gui.fill(x - 760 + inset, y - 932 + h - LABEL_HEIGHT,
                x - 760 + w - inset, y - 932 + h - inset, 0x66000000);
    }

    private void icon(GuiGraphics gui, String name, int x, int y, int w, int h, float alpha) {
        float scale = panelScale();
        double pixelScale = scale * minecraft.getWindow().getGuiScale();
        var texture = InventorySvgAssets.get(name, Math.max(1, (int) Math.ceil(w * pixelScale)),
                Math.max(1, (int) Math.ceil(h * pixelScale)));
        // Flush the slot fills before switching shader color and drawing the SVG texture.
        gui.flush();
        gui.setColor(1, 1, 1, alpha);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        gui.blit(texture.id(), x - 760, y - 932, w, h, 0, 0,
                texture.width(), texture.height(), texture.width(), texture.height());
        gui.setColor(1, 1, 1, 1);
        RenderSystem.disableBlend();
    }

    @Override
    public void removed() {
        InventorySvgAssets.clear();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_TAB || InventoryPreviewKeys.OPEN.matches(keyCode, scanCode)) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
