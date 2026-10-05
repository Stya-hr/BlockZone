package dev.stya.blockzone.client.editor;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.List;
import java.util.Locale;

final class LootTablePickerScreen extends Screen {
    private final LootCrateEditorScreen parent;
    private final List<String> tables;
    private List<String> visible;
    private int scroll;
    LootTablePickerScreen(LootCrateEditorScreen parent, List<String> tables) {
        super(Component.translatable("editor.blockzone.loot.choose_title"));
        this.parent = parent; this.tables = tables; visible = tables;
    }
    @Override protected void init() {
        var search = addRenderableWidget(new EditBox(font, 20, 30, width - 40, 20, title));
        search.setResponder(value -> { visible = tables.stream().filter(id -> id.contains(value.toLowerCase(Locale.ROOT))).toList(); scroll = 0; });
        addRenderableWidget(Button.builder(Component.translatable("gui.back"), b -> onClose()).bounds(width / 2 - 50, height - 28, 100, 20).build());
        setInitialFocus(search);
    }
    @Override public void render(GuiGraphics graphics, int x, int y, float partial) {
        renderBackground(graphics); graphics.drawCenteredString(font, title, width / 2, 10, 0xffffff);
        int rows = Math.max(1, (height - 100) / 20);
        scroll = Math.max(0, Math.min(scroll, Math.max(0, visible.size() - rows)));
        for (int i = scroll; i < Math.min(visible.size(), scroll + rows); i++) {
            int rowY = 60 + (i - scroll) * 20;
            if (y >= rowY && y < rowY + 20) graphics.fill(20, rowY, width - 20, rowY + 20, 0xff355d46);
            graphics.drawString(font, font.plainSubstrByWidth(visible.get(i), width - 48), 24, rowY + 5, 0xffffff);
        }
        super.render(graphics, x, y, partial);
    }
    @Override public boolean mouseClicked(double x, double y, int button) {
        if (button == 0 && x >= 20 && x <= width - 20 && y >= 60 && y < height - 40) {
            int i = scroll + (int)((y - 60) / 20);
            if (i < visible.size() && i < scroll + Math.max(1, (height - 100) / 20)) parent.pick(visible.get(i)); return true;
        }
        return super.mouseClicked(x, y, button);
    }
    @Override public boolean mouseScrolled(double x, double y, double delta) { scroll -= (int)delta; return true; }
    @Override public void onClose() { minecraft.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }
}
