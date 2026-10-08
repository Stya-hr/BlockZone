package dev.stya.blockzone.client.editor;

import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

final class LootTablePickerScreen extends Screen {
    private static final int ROW_HEIGHT = 32;
    private final LootCrateEditorScreen parent;
    private final List<String> tables;
    private List<String> visible;
    private EditBox search;
    private String searchValue = "";
    private int left, top, dialogWidth, dialogHeight, listTop, listBottom, scroll, highlighted = -1;
    private boolean draggingScroll;

    LootTablePickerScreen(LootCrateEditorScreen parent, List<String> tables) {
        super(Component.translatable("editor.blockzone.loot.choose_title"));
        this.parent = parent;
        this.tables = tables;
        visible = tables;
        highlighted = tables.isEmpty() ? -1 : Math.max(0, tables.indexOf(parent.selectedTable()));
        scroll = Math.max(0, highlighted * ROW_HEIGHT);
    }

    private Component ui(String key, Object... args) { return Component.translatable("editor.blockzone.loot.ui." + key, args); }

    @Override
    protected void init() {
        dialogWidth = Math.min(460, width - 24);
        dialogHeight = Math.min(372, height - 24);
        left = (width - dialogWidth) / 2;
        top = (height - dialogHeight) / 2;
        listTop = top + 96;
        listBottom = top + dialogHeight - 45;
        search = addRenderableWidget(EditorUi.input(font, left + 14, top + 48, dialogWidth - 28, 22, ui("search_tables")));
        search.setMaxLength(256);
        search.setValue(searchValue);
        search.setHint(ui("search_tables"));
        search.setTooltip(Tooltip.create(ui("search_tables")));
        search.setResponder(value -> {
            searchValue = value;
            String query = value.strip().toLowerCase(Locale.ROOT);
            visible = tables.stream().filter(id -> id.toLowerCase(Locale.ROOT).contains(query)).toList();
            scroll = 0;
            highlighted = visible.isEmpty() ? -1 : 0;
        });
        addRenderableWidget(EditorUi.button(left + 14, top + dialogHeight - 33, 88, 22,
                Component.translatable("gui.back"), EditorUi.Tone.DEFAULT, this::onClose));
        clampScroll();
        setInitialFocus(search);
    }

    @Override public void tick() { search.tick(); }

    private int listHeight() { return Math.max(1, listBottom - listTop); }

    private void clampScroll() { scroll = Mth.clamp(scroll, 0, Math.max(0, visible.size() * ROW_HEIGHT - listHeight())); }

    private boolean inList(double x, double y) {
        return x >= left + 12 && x < left + dialogWidth - 12 && y >= listTop && y < listBottom;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xb009111b);
        EditorUi.panel(graphics, left, top, dialogWidth, dialogHeight);
        graphics.fill(left + 1, top + 1, left + dialogWidth - 1, top + 3, EditorUi.ACCENT);
        EditorUi.text(graphics, font, title, left + 14, top + 14, dialogWidth - 28, EditorUi.TEXT);
        EditorUi.text(graphics, font, ui("choose_hint"), left + 14, top + 31, dialogWidth - 28, EditorUi.MUTED);
        EditorUi.section(graphics, font, ui("results", visible.size(), tables.size()), left + 14, top + 81, dialogWidth - 28);
        clampScroll();
        Component tooltip = null;
        graphics.enableScissor(left + 12, listTop, left + dialogWidth - 12, listBottom);
        if (visible.isEmpty()) {
            int y = listTop + Math.max(8, (listHeight() - 31) / 2);
            EditorUi.text(graphics, font, ui("tables_empty"), left + 22, y, dialogWidth - 44, EditorUi.TEXT);
            EditorUi.text(graphics, font, ui("tables_empty_hint"), left + 22, y + 17, dialogWidth - 44, EditorUi.MUTED);
            if (inList(mouseX, mouseY)) tooltip = ui("tables_empty_hint");
        }
        for (int i = scroll / ROW_HEIGHT; i < visible.size(); i++) {
            int y = listTop + i * ROW_HEIGHT - scroll;
            if (y >= listBottom) break;
            String id = visible.get(i);
            boolean current = id.equals(parent.selectedTable());
            boolean hovered = inList(mouseX, mouseY) && mouseX >= left + 13 && mouseX < left + dialogWidth - 21
                    && mouseY >= y + 1 && mouseY < y + ROW_HEIGHT - 2;
            int x = left + 13, rowWidth = dialogWidth - 34;
            graphics.fill(x, y + 1, x + rowWidth, y + ROW_HEIGHT - 2,
                    current ? 0xe8244945 : hovered ? EditorUi.HOVER : EditorUi.SURFACE);
            if (current) graphics.fill(x, y + 1, x + 2, y + ROW_HEIGHT - 2, EditorUi.ACCENT);
            if (highlighted == i) graphics.renderOutline(x, y + 1, rowWidth, ROW_HEIGHT - 3, EditorUi.ACCENT);
            int separator = id.indexOf(':');
            String namespace = separator < 0 ? "minecraft" : id.substring(0, separator);
            String path = separator < 0 ? id : id.substring(separator + 1);
            int badgeWidth = current ? font.width(ui("current")) + 14 : 0;
            EditorUi.text(graphics, font, Component.literal(path), x + 9, y + 5, rowWidth - 18 - badgeWidth, current ? EditorUi.ACCENT : EditorUi.TEXT);
            EditorUi.text(graphics, font, Component.literal(namespace), x + 9, y + 18, rowWidth - 18, EditorUi.MUTED);
            if (current) EditorUi.text(graphics, font, ui("current"), x + rowWidth - badgeWidth + 3, y + 5, badgeWidth - 8, EditorUi.ACCENT);
            if (hovered) tooltip = Component.literal(id);
        }
        graphics.disableScissor();
        EditorUi.scrollbar(graphics, left + dialogWidth - 16, listTop, listHeight(), visible.size() * ROW_HEIGHT, scroll);
        graphics.fill(left + 14, top + dialogHeight - 42, left + dialogWidth - 14, top + dialogHeight - 41, EditorUi.BORDER);
        EditorUi.text(graphics, font, ui("keyboard_hint"), left + 115, top + dialogHeight - 25, dialogWidth - 129, EditorUi.MUTED);
        super.render(graphics, mouseX, mouseY, partialTick);
        if (tooltip != null) graphics.renderTooltip(font, tooltip, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        if (button == 0 && inList(x, y)) {
            if (x >= left + dialogWidth - 21) {
                if (visible.size() * ROW_HEIGHT > listHeight()) {
                    draggingScroll = true;
                    scrollTo(y);
                }
            } else if (x >= left + 13) {
                int index = (int) (y - listTop + scroll) / ROW_HEIGHT;
                int rowOffset = (int) (y - listTop + scroll) % ROW_HEIGHT;
                if (index >= 0 && index < visible.size() && rowOffset >= 1 && rowOffset < ROW_HEIGHT - 2)
                    parent.pick(visible.get(index));
            }
            return true;
        }
        return super.mouseClicked(x, y, button);
    }

    private void scrollTo(double y) {
        scroll = (int) ((y - listTop) / listHeight() * Math.max(0, visible.size() * ROW_HEIGHT - listHeight()));
        clampScroll();
    }

    @Override
    public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
        if (draggingScroll) { scrollTo(y); return true; }
        return super.mouseDragged(x, y, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double x, double y, int button) {
        if (draggingScroll) { draggingScroll = false; return true; }
        return super.mouseReleased(x, y, button);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double delta) {
        if (!inList(x, y)) return super.mouseScrolled(x, y, delta);
        scroll -= (int) Math.round(delta * ROW_HEIGHT);
        clampScroll();
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        if ((key == GLFW.GLFW_KEY_UP || key == GLFW.GLFW_KEY_DOWN) && search.isFocused() && !visible.isEmpty()) {
            highlighted = Mth.clamp(highlighted + (key == GLFW.GLFW_KEY_DOWN ? 1 : -1), 0, visible.size() - 1);
            int y = highlighted * ROW_HEIGHT;
            if (y < scroll) scroll = y;
            if (y + ROW_HEIGHT > scroll + listHeight()) scroll = y + ROW_HEIGHT - listHeight();
            clampScroll();
            return true;
        }
        if ((key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER)
                && search.isFocused() && highlighted >= 0 && highlighted < visible.size()) {
            parent.pick(visible.get(highlighted));
            return true;
        }
        return super.keyPressed(key, scanCode, modifiers);
    }

    @Override public void onClose() { minecraft.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }
}
