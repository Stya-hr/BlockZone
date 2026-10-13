package dev.stya.blockzone.client.inventory;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.stya.blockzone.inventory.InventoryPanelLayout;
import dev.stya.blockzone.inventory.InventorySlot;
import dev.stya.blockzone.inventory.PlayerPanelInventory;
import dev.stya.blockzone.net.battlezone.BattlezoneNetwork;
import dev.stya.blockzone.net.battlezone.DiscardInventoryC2SPacket;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** Non-pausing, server-authoritative inventory with shared render/hit-test geometry. */
public final class InventoryPanelScreen extends Screen {
    private static final int FOREGROUND = 0xFFF3F7F8;
    private static final int SLOT_ICON_SIZE = 40;
    private static final int CAPTION_HEIGHT = 30;
    private static final int GUN_MARGIN_X = 12;
    private static final int GUN_MARGIN_Y = 6;
    private InventoryPanelLayout.Cell hovered;
    private Component attachmentName;
    private boolean preserveSneak;
    private InventoryPanelLayout.Cell pressedCell;
    private java.util.UUID pressedRound;
    private int[] pressedRevisions;
    private int pressedAction;
    private int pressedButton = -1;
    private double pressX, pressY;
    private boolean dragging;

    public InventoryPanelScreen() {
        super(Component.translatable("screen.blockzone.inventory_preview"));
        preserveSneak = net.minecraft.client.Minecraft.getInstance().options.keyShift.isDown();
    }
    @Override protected void init() {
        if (preserveSneak) minecraft.options.keyShift.setDown(true);
        preserveSneak = false;
    }
    private float scale() { return 1.2F * Math.min(width / 2560F, height / 1440F); }
    private float originX() { return (width - 1040 * scale()) / 2; }
    private float originY() { return height - 532 * scale(); }
    private InventoryPanelLayout.Cell at(double x, double y) {
        return InventoryPanelLayout.at((x - originX()) / scale(), (y - originY()) / scale());
    }
    private PlayerPanelInventory data() {
        return minecraft.player == null ? null : PlayerPanelInventory.get(minecraft.player).filter(PlayerPanelInventory::active).orElse(null);
    }
    private ItemStack stack(InventorySlot slot) { var data = data(); return data == null ? ItemStack.EMPTY : data.getStack(slot); }

    @Override public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        hovered = at(mouseX, mouseY);
        attachmentName = null;
        gui.pose().pushPose(); gui.pose().translate(originX(), originY(), 0); gui.pose().scale(scale(), scale(), 1);
        double px = (mouseX - originX()) / scale(), py = (mouseY - originY()) / scale();
        for (var cell : InventoryPanelLayout.CELLS) {
            var item = stack(cell.slot());
            box(gui, cell.x(), cell.y(), cell.width(), cell.height(), cell == hovered || dragging && cell == pressedCell);
            boolean equipment = InventorySlot.FIRST_ROW.contains(cell.slot());
            boolean weapon = InventorySlot.WEAPONS.contains(cell.slot());
            int imageHeight = cell.height() - (equipment ? 0 : CAPTION_HEIGHT);
            if (!equipment) gui.fill(cell.x() + 3, cell.y() + cell.height() - CAPTION_HEIGHT, cell.x() + cell.width() - 3, cell.y() + cell.height() - 3, 0x66000000);
            if (item.isEmpty()) {
                int w = weapon ? 156 : SLOT_ICON_SIZE;
                int h = weapon ? 56 : SLOT_ICON_SIZE;
                icon(gui, cell.slot().icon(), cell.x() + (cell.width() - w) / 2, cell.y() + (imageHeight - h) / 2, w, h);
            } else if (!weapon || !GunHudIcon.draw(gui, item, cell.x() + GUN_MARGIN_X, cell.y() + GUN_MARGIN_Y, cell.width() - 2 * GUN_MARGIN_X, imageHeight - 2 * GUN_MARGIN_Y)) {
                gui.pose().pushPose();
                gui.pose().translate(cell.x() + (cell.width() - SLOT_ICON_SIZE) / 2F, cell.y() + (imageHeight - SLOT_ICON_SIZE) / 2F, 0);
                gui.pose().scale(SLOT_ICON_SIZE / 16F, SLOT_ICON_SIZE / 16F, 1); gui.renderItem(item, 0, 0); gui.pose().popPose();
            }
            if (weapon) {
                String label = font.plainSubstrByWidth((item.isEmpty() ? slotName(cell.slot()) : item.getHoverName()).getString(), (cell.width() - 12) / 2);
                text(gui, label, cell.x() + (cell.width() - font.width(label) * 2F) / 2, cell.y() + cell.height() - 25, 2F, FOREGROUND);
            } else if (!equipment) {
                int limit = data() == null ? cell.slot().defaultLimit() : data().limit(cell.slot());
                String count = item.getCount() + " / " + limit;
                text(gui, count, cell.x() + (cell.width() - font.width(count) * 2.2F) / 2, cell.y() + cell.height() - 26, 2.2F,
                        item.getCount() > limit ? 0xFFFF9E81 : FOREGROUND);
            }
        }
        // Draw flat attachment status last, above every item/HUD render pass.
        gui.flush();
        gui.pose().pushPose(); gui.pose().translate(0, 0, 400);
        for (var cell : InventoryPanelLayout.CELLS) if (InventorySlot.WEAPONS.contains(cell.slot()))
            attachments(gui, cell, GunAttachmentView.of(stack(cell.slot())), px, py);
        gui.flush(); gui.pose().popPose();
        Component name = attachmentName != null ? attachmentName : hovered == null ? getTitle()
                : stack(hovered.slot()).isEmpty() ? slotName(hovered.slot()) : stack(hovered.slot()).getHoverName();
        String description = font.plainSubstrByWidth(name.getString(), (int) ((1040 - 24) / 2.2F));
        float descriptionWidth = font.width(description) * 2.2F;
        int frameWidth = (int) Math.ceil(descriptionWidth + 24);
        box(gui, (1040 - frameWidth) / 2, 0, frameWidth, 34, false);
        text(gui, description, (1040 - descriptionWidth) / 2, (34 - font.lineHeight * 2.2F) / 2, 2.2F, FOREGROUND);
        InventoryPromptRenderer.render(gui, font, InventoryPreviewKeys.bulkDropDown(minecraft));
        gui.pose().popPose();
        if (dragging && pressedCell != null) {
            gui.pose().pushPose(); gui.pose().translate(0, 0, 500);
            gui.renderItem(stack(pressedCell.slot()), mouseX - 8, mouseY - 8);
            gui.pose().popPose();
        }
    }

    private Component slotName(InventorySlot slot) { return Component.translatable("slot.blockzone." + slot.id()); }
    private void attachments(GuiGraphics gui, InventoryPanelLayout.Cell cell, List<GunAttachmentView.Attachment> attachments, double px, double py) {
        int size = 24, gap = 4;
        int rowWidth = attachments.size() * size + Math.max(0, attachments.size() - 1) * gap;
        int x = cell.x() + cell.width() - GUN_MARGIN_X - rowWidth;
        for (var attachment : attachments) {
            int y = cell.y() + GUN_MARGIN_Y;
            boolean pointed = px >= x && px < x + size && py >= y && py < y + size;
            box(gui, x, y, size, size, pointed);
            icon(gui, attachment.icon(), x + 4, y + 4, 16, 16, attachment.item().isEmpty() ? .25F : .9F);
            if (pointed) attachmentName = attachment.item().isEmpty() ? attachment.name() : attachment.item().getHoverName();
            x += size + gap;
        }
    }

    private void text(GuiGraphics gui, String value, float x, float y, float scale, int color) {
        gui.pose().pushPose(); gui.pose().translate(x, y, 0); gui.pose().scale(scale, scale, 1);
        gui.drawString(font, value, 0, 0, color, false); gui.pose().popPose();
    }
    private void box(GuiGraphics gui, int x, int y, int w, int h, boolean pointed) {
        gui.fill(x, y, x + w, y + h, 0x8C000000);
        if (pointed) gui.fill(x + 3, y + 3, x + w - 3, y + h - 3, 0x335D7888);
        float stroke = Math.max(2F, 1.5F / (scale() * (float) minecraft.getWindow().getGuiScale()));
        gui.pose().pushPose(); gui.pose().translate(x, y, 0); gui.pose().scale(stroke, stroke, 1);
        int ew = Math.round(w / stroke), eh = Math.round(h / stroke), color = pointed ? FOREGROUND : 0x73666666;
        gui.fill(0, 0, ew, 1, color); gui.fill(0, eh - 1, ew, eh, color);
        gui.fill(0, 0, 1, eh, color); gui.fill(ew - 1, 0, ew, eh, color); gui.pose().popPose();
    }
    private void icon(GuiGraphics gui, String name, int x, int y, int w, int h) {
        icon(gui, name, x, y, w, h, .25F);
    }
    private void icon(GuiGraphics gui, String name, int x, int y, int w, int h, float opacity) {
        double pixelScale = scale() * minecraft.getWindow().getGuiScale();
        var texture = InventorySvgAssets.get(name, Math.max(1, (int) Math.ceil(w * pixelScale)), Math.max(1, (int) Math.ceil(h * pixelScale)));
        gui.flush(); gui.setColor(1, 1, 1, opacity); RenderSystem.enableBlend(); RenderSystem.defaultBlendFunc();
        gui.blit(texture.id(), x, y, w, h, 0, 0, texture.width(), texture.height(), texture.width(), texture.height());
        gui.flush(); gui.setColor(1, 1, 1, 1); RenderSystem.disableBlend();
    }
    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (minecraft.options.keyInventory.matchesMouse(button)) { onClose(); return true; }
        var cell = at(mouseX, mouseY);
        if (cell == null) { onClose(); return true; }
        boolean all = minecraft.options.keyUse.matchesMouse(button);
        if (!all && !minecraft.options.keyAttack.matchesMouse(button)) return false;
        if (all) { discard(cell, true); return true; }
        var data = data();
        if (data == null || stack(cell.slot()).isEmpty()) return true;
        pressedCell = cell; pressedButton = button; pressedRound = data.round();
        pressedRevisions = InventorySlot.ALL.stream().mapToInt(data::revision).toArray();
        pressedAction = InventoryPreviewKeys.bulkDropDown(minecraft) ? 1 : 0;
        pressX = mouseX; pressY = mouseY; dragging = false;
        return true;
    }
    @Override public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        if (pressedCell == null || button != pressedButton) return super.mouseDragged(mouseX, mouseY, button, dx, dy);
        if (Math.hypot(mouseX - pressX, mouseY - pressY) * minecraft.getWindow().getGuiScale() >= 4) dragging = true;
        return true;
    }
    @Override public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (pressedCell == null || button != pressedButton) return super.mouseReleased(mouseX, mouseY, button);
        var target = at(mouseX, mouseY);
        if (target != null && target != pressedCell) dragging = true;
        if (dragging) {
            if (target != null && target != pressedCell)
                BattlezoneNetwork.swapInventory(new dev.stya.blockzone.net.battlezone.SwapInventoryC2SPacket(
                        pressedCell.slot().index(), target.slot().index(), pressedRound,
                        pressedRevisions[pressedCell.slot().index()], pressedRevisions[target.slot().index()]));
        } else if (target == pressedCell) {
            BattlezoneNetwork.discard(new DiscardInventoryC2SPacket(pressedCell.slot().index(), pressedAction,
                    pressedRound, pressedRevisions[pressedCell.slot().index()]));
        }
        pressedCell = null; pressedRevisions = null; pressedButton = -1; dragging = false;
        return true;
    }
    private void discard(InventoryPanelLayout.Cell cell, boolean all) {
        if (cell == null) return;
        var data = data(); var stack = stack(cell.slot());
        if (data != null && !stack.isEmpty()) BattlezoneNetwork.discard(new DiscardInventoryC2SPacket(
                cell.slot().index(), all ? 2 : InventoryPreviewKeys.bulkDropDown(minecraft) ? 1 : 0, data.round(), data.revision(cell.slot())));
    }
    @Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (InventoryPreviewKeys.movementKeys(minecraft).stream().anyMatch(key -> key.matches(keyCode, scanCode))) {
            onClose(); return false; // Forward the same movement key after KeyboardHandler sees screen == null.
        }
        if (minecraft.options.keyInventory.matches(keyCode, scanCode)) { onClose(); return true; }
        boolean all = minecraft.options.keyUse.matches(keyCode, scanCode);
        if (all || minecraft.options.keyAttack.matches(keyCode, scanCode)) { discard(hovered, all); return true; }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    @Override public void removed() { InventorySvgAssets.clear(); }
    @Override public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (minecraft.options.keyShift.matches(keyCode, scanCode)) minecraft.options.keyShift.setDown(false);
        return super.keyReleased(keyCode, scanCode, modifiers);
    }
    @Override public boolean isPauseScreen() { return false; }
}
