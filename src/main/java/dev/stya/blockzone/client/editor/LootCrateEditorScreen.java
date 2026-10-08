package dev.stya.blockzone.client.editor;

import dev.stya.blockzone.editor.loot.LootCrateDraft;
import dev.stya.blockzone.editor.loot.LootCrateEdit;
import dev.stya.blockzone.editor.loot.LootEditorProjection;
import dev.stya.blockzone.editor.zone.PoisonEditorGeometry;
import dev.stya.blockzone.net.battlezone.BattlezoneNetwork;
import dev.stya.blockzone.net.editor.LootEditorResultS2CPacket;
import dev.stya.blockzone.net.editor.LootEditorS2CPacket;
import dev.stya.blockzone.net.editor.SaveLootEditorC2SPacket;
import dev.stya.blockzone.zone.ZoneGeometry;
import dev.stya.blockzone.zone.ZoneShape;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

public final class LootCrateEditorScreen extends Screen {
    private static final int PANE_X = 10;
    private static final int PANE_TOP = 88;
    private static final int ROW_HEIGHT = 34;
    private static final int CONTENT_HEIGHT = 300;
    private final LootEditorS2CPacket packet;
    private final LootCrateDraft draft;
    private final List<AbstractWidget> contentWidgets = new ArrayList<>();
    private final List<AbstractWidget> controls = new ArrayList<>();
    private final Map<AbstractWidget, Integer> contentPositions = new IdentityHashMap<>();
    private EditBox filter, table, seed;
    private Checkbox close, enabled;
    private Button save, apply, selectAll, clear;
    private String filterValue = "";
    private String tableValue = "blockzone:chests/common", seedValue = "0";
    String message = "";
    private boolean closeValue = true, enabledValue, list, saving, dragging, rotating;
    private boolean draggingContentScroll, draggingListScroll;
    private int right, paneWidth, paneHeight, contentX, contentWidth, contentTop, contentBottom;
    private int scroll, contentScroll;
    private double startX, startY, endX, endY;
    private boolean add;
    private Component hoverText;

    public static void open(LootEditorS2CPacket packet) {
        var mc = Minecraft.getInstance();
        if (mc.level == null || !mc.level.dimension().location().equals(packet.dimension())) return;
        var screen = new LootCrateEditorScreen(packet);
        if (!LootWorldEditor.start(screen)) screen.message = Component.translatable("editor.blockzone.camera_busy").getString();
        mc.setScreen(screen);
    }

    public static void result(LootEditorResultS2CPacket packet) {
        if (Minecraft.getInstance().screen instanceof LootCrateEditorScreen screen && screen.packet.token().equals(packet.token())) {
            screen.saving = false;
            screen.message = Component.translatable(packet.messageKey()).getString();
            if (packet.saved()) screen.draft.markSaved();
        }
    }

    private LootCrateEditorScreen(LootEditorS2CPacket packet) {
        super(Component.translatable("editor.blockzone.loot.title", packet.mapName()));
        this.packet = packet;
        draft = new LootCrateDraft(packet.crates());
    }

    private Component text(String key) { return Component.translatable("editor.blockzone.loot." + key); }
    private Component ui(String key, Object... args) { return Component.translatable("editor.blockzone.loot.ui." + key, args); }

    private Button button(int x, int y, int width, String key, EditorUi.Tone tone, Runnable action) {
        var widget = EditorUi.button(x, y, width, 22, text(key), tone, () -> {
            if (!saving) action.run();
        });
        controls.add(widget);
        return addRenderableWidget(widget);
    }

    private <T extends AbstractWidget> T content(T widget, int y) {
        contentWidgets.add(widget);
        controls.add(widget);
        contentPositions.put(widget, y);
        return addWidget(widget);
    }

    private Button contentButton(int x, int y, int width, String key, EditorUi.Tone tone, Runnable action) {
        return content(EditorUi.button(x, 0, width, 22, text(key), tone, () -> {
            if (!saving) action.run();
        }), y);
    }

    @Override
    protected void init() {
        rememberOptions();
        dragging = rotating = draggingContentScroll = draggingListScroll = false;
        setDragging(false);
        contentWidgets.clear();
        controls.clear();
        contentPositions.clear();
        right = width - EditorUi.sidebarWidth(width);
        paneWidth = Math.max(1, right - PANE_X * 2);
        paneHeight = Math.max(1, height - PANE_TOP - 52);
        contentX = right + 12;
        contentWidth = width - contentX - 12;
        contentTop = 60;
        contentBottom = Math.max(contentTop + 24, height - 74);

        filter = addRenderableWidget(EditorUi.input(font, PANE_X + 4, 31, Math.max(1, paneWidth - 8), 22, text("filter")));
        controls.add(filter);
        filter.setValue(filterValue);
        filter.setHint(text("filter"));
        filter.setTooltip(Tooltip.create(text("filter")));
        filter.setResponder(value -> {
            filterValue = value;
            draft.filter(value);
            scroll = 0;
        });
        int modeWidth = (paneWidth - 4) / 2;
        button(PANE_X, 59, modeWidth, "ui.world", list ? EditorUi.Tone.DEFAULT : EditorUi.Tone.PRIMARY, () -> setView(false))
                .setTooltip(Tooltip.create(text("overview")));
        button(PANE_X + modeWidth + 4, 59, paneWidth - modeWidth - 4, "ui.list", list ? EditorUi.Tone.PRIMARY : EditorUi.Tone.DEFAULT, () -> setView(true))
                .setTooltip(Tooltip.create(text("list")));

        table = content(EditorUi.input(font, contentX, 0, contentWidth - 38, 22, text("table")), 34);
        table.setMaxLength(256);
        table.setValue(tableValue);
        table.setTooltip(Tooltip.create(Component.literal(tableValue)));
        table.setResponder(value -> {
            tableValue = value;
            table.setTooltip(Tooltip.create(Component.literal(value)));
        });
        contentButton(contentX + contentWidth - 32, 34, 32, "choose", EditorUi.Tone.DEFAULT, () -> {
            rememberOptions();
            minecraft.setScreen(new LootTablePickerScreen(this, packet.tables()));
        });
        seed = content(EditorUi.input(font, contentX, 0, contentWidth, 22, text("seed")), 78);
        seed.setMaxLength(20);
        seed.setValue(seedValue);
        seed.setResponder(value -> seedValue = value);
        enabled = content(EditorUi.toggle(contentX, 0, contentWidth, text("enabled"), enabledValue), 110);
        close = content(EditorUi.toggle(contentX, 0, contentWidth, text("close"), closeValue), 138);
        apply = contentButton(contentX, 170, contentWidth, "apply", EditorUi.Tone.PRIMARY, this::applyDraft);
        int half = (contentWidth - 6) / 2;
        selectAll = contentButton(contentX, 270, half, "all", EditorUi.Tone.DEFAULT, draft::selectAll);
        clear = contentButton(contentX + half + 6, 270, contentWidth - half - 6, "clear", EditorUi.Tone.DEFAULT, draft::clearSelection);

        save = button(contentX, height - 34, half, "save", EditorUi.Tone.PRIMARY, () -> {
            if (!draft.dirty()) return;
            saving = true;
            message = text("saving").getString();
            BattlezoneNetwork.saveLootEditor(new SaveLootEditorC2SPacket(packet.token(), draft.changes()));
        });
        button(contentX + half + 6, height - 34, contentWidth - half - 6, "exit", EditorUi.Tone.DEFAULT, this::onClose);
        positionContent();
        updateControls();
    }

    private void setView(boolean value) {
        if (list == value) return;
        list = value;
        dragging = rotating = false;
        LootEditorCamera.clearMovement();
        rebuildWidgets();
        setFocused(null);
    }

    private void rememberOptions() {
        if (close != null) closeValue = close.selected();
        if (enabled != null) enabledValue = enabled.selected();
    }

    private void applyDraft() {
        try {
            if (!packet.tables().contains(tableValue.strip())) throw new IllegalArgumentException(text("invalid_table").getString());
            draft.apply(tableValue, Long.parseLong(seedValue.strip()), close.selected(), enabled.selected());
            rememberOptions();
            message = text("draft_applied").getString();
        } catch (NumberFormatException failure) {
            message = text("invalid_seed").getString();
        } catch (IllegalArgumentException failure) {
            message = failure.getMessage();
        }
    }

    private void updateControls() {
        controls.forEach(widget -> widget.active = !saving);
        filter.setEditable(!saving);
        table.setEditable(!saving);
        seed.setEditable(!saving);
        save.active = !saving && draft.dirty();
        apply.active = !saving && draft.selectedCount() > 0;
        selectAll.active = !saving && !draft.visible().isEmpty();
        clear.active = !saving && draft.selectedCount() > 0;
    }

    private void positionContent() {
        contentScroll = Mth.clamp(contentScroll, 0, Math.max(0, CONTENT_HEIGHT - contentBottom + contentTop));
        for (var widget : contentWidgets) widget.setY(contentTop + contentPositions.get(widget) - contentScroll);
    }

    private void revealFocusedContent() {
        if (!(getFocused() instanceof AbstractWidget widget) || !contentPositions.containsKey(widget)) return;
        int y = contentPositions.get(widget);
        int labelHeight = widget instanceof EditBox ? 16 : 4;
        if (y - labelHeight < contentScroll) contentScroll = y - labelHeight;
        else if (y + widget.getHeight() > contentScroll + contentBottom - contentTop)
            contentScroll = y + widget.getHeight() - (contentBottom - contentTop) + 4;
        positionContent();
    }

    void pick(String value) { tableValue = value; minecraft.setScreen(this); }
    String selectedTable() { return tableValue.strip(); }

    @Override
    public void tick() {
        filter.tick();
        table.tick();
        seed.tick();
        rememberOptions();
        updateControls();
    }

    LootEditorS2CPacket packet() { return packet; }
    LootCrateDraft draft() { return draft; }
    double mapTop() { return Math.max(packet.pos1().getY(), packet.pos2().getY()); }

    ZoneGeometry focusArea() {
        var selected = draft.entries().stream().filter(draft::selected).toList();
        var crates = selected.isEmpty() ? draft.entries() : selected;
        double minX = crates.stream().mapToDouble(LootCrateEdit::x).min().orElse(Math.min(packet.pos1().getX(), packet.pos2().getX()));
        double maxX = crates.stream().mapToDouble(LootCrateEdit::x).max().orElse(Math.max(packet.pos1().getX(), packet.pos2().getX()));
        double minZ = crates.stream().mapToDouble(LootCrateEdit::z).min().orElse(Math.min(packet.pos1().getZ(), packet.pos2().getZ()));
        double maxZ = crates.stream().mapToDouble(LootCrateEdit::z).max().orElse(Math.max(packet.pos1().getZ(), packet.pos2().getZ()));
        double y = crates.stream().mapToDouble(LootCrateEdit::y).max().orElse(packet.pos1().getY()) + .5;
        double radius = Math.max(2, Math.max(maxX - minX, maxZ - minZ) / 2 + 2);
        var mc = Minecraft.getInstance();
        int guiWidth = mc.getWindow().getGuiScaledWidth();
        int guiHeight = mc.getWindow().getGuiScaledHeight();
        // Camera ownership starts before Screen.init(), so derive the same sidebar width from the window.
        var area = new ZoneGeometry((minX + maxX) / 2 + .5, y, (minZ + maxZ) / 2 + .5, radius, ZoneShape.CYLINDER);
        return PoisonEditorGeometry.focusForViewport(area, mapTop(), mc.options.fov().get(), guiWidth, guiHeight,
                (guiWidth - EditorUi.sidebarWidth(guiWidth)) / 2.0);
    }

    private LootEditorProjection.Point project(LootCrateEdit entry) {
        var camera = minecraft.gameRenderer.getMainCamera();
        return LootEditorProjection.project(new net.minecraft.world.phys.Vec3(entry.x() + .5, entry.y() + .5, entry.z() + .5),
                camera.getPosition(), camera.rotation(), minecraft.options.fov().get(), width, height);
    }

    private boolean visiblePoint(LootEditorProjection.Point point) { return point != null && inPane(point.x(), point.y()); }
    private boolean inPane(double x, double y) {
        return x >= PANE_X && x < PANE_X + paneWidth && y >= PANE_TOP && y < PANE_TOP + paneHeight;
    }
    private boolean inContent(double x, double y) {
        return x >= right && x < width && y >= contentTop && y < contentBottom;
    }

    private void label(GuiGraphics graphics, Component text, int x, int y, int maxWidth, int color, int mouseX, int mouseY) {
        EditorUi.text(graphics, font, text, x, y, maxWidth, color);
        if (font.width(text) > maxWidth && mouseX >= x && mouseX < x + maxWidth && mouseY >= y && mouseY < y + font.lineHeight)
            hoverText = text;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        hoverText = null;
        updateControls();
        EditorUi.panel(graphics, right, 0, width - right, height);
        graphics.fill(right, 0, right + 2, 48, EditorUi.ACCENT);
        label(graphics, ui("name"), contentX, 14, contentWidth, EditorUi.TEXT, mouseX, mouseY);
        label(graphics, Component.literal(packet.mapName()), contentX, 31, contentWidth, EditorUi.MUTED, mouseX, mouseY);
        graphics.fill(contentX, 49, width - 12, 50, EditorUi.BORDER);

        EditorUi.panel(graphics, PANE_X - 4, 8, paneWidth + 8, 48);
        label(graphics, ui("crates"), PANE_X + 4, 16, paneWidth - 8, EditorUi.MUTED, mouseX, mouseY);
        var visible = draft.visible();
        if (list) renderList(graphics, visible, mouseX, mouseY);
        else renderWorldSelection(graphics, visible, mouseX, mouseY);

        graphics.enableScissor(right + 8, contentTop, width - 8, contentBottom);
        int y = contentTop - contentScroll;
        boolean contentHovered = inContent(mouseX, mouseY);
        int contentMouseX = contentHovered ? mouseX : -1;
        int contentMouseY = contentHovered ? mouseY : -1;
        EditorUi.section(graphics, font, ui("properties"), contentX, y + 2, contentWidth);
        EditorUi.text(graphics, font, text("table"), contentX, y + 21, contentWidth, EditorUi.TEXT);
        EditorUi.text(graphics, font, text("seed"), contentX, y + 65, contentWidth, EditorUi.TEXT);
        EditorUi.section(graphics, font, ui("selection"), contentX, y + 211, contentWidth);
        EditorUi.card(graphics, contentX, y + 227, contentWidth, 36);
        label(graphics, Component.translatable("editor.blockzone.loot.selected", draft.selectedCount(), draft.entries().size()),
                contentX + 7, y + 234, contentWidth - 14, EditorUi.TEXT, contentMouseX, contentMouseY);
        label(graphics, ui("visible", visible.size()), contentX + 7, y + 248, contentWidth - 14, EditorUi.MUTED, contentMouseX, contentMouseY);
        for (var widget : contentWidgets) {
            if (widget.getY() < contentBottom && widget.getY() + widget.getHeight() > contentTop)
                widget.render(graphics, contentMouseX, contentMouseY, partialTick);
        }
        graphics.disableScissor();
        EditorUi.scrollbar(graphics, width - 6, contentTop, contentBottom - contentTop, CONTENT_HEIGHT, contentScroll);

        graphics.fill(contentX, height - 65, width - 12, height - 64, EditorUi.BORDER);
        Component status = message.isBlank()
                ? draft.dirty() ? Component.translatable("editor.blockzone.loot.changed", draft.changes().size()) : ui("no_changes")
                : Component.literal(message);
        label(graphics, status, contentX, height - 53, contentWidth,
                message.isBlank() ? draft.dirty() ? EditorUi.ACCENT : EditorUi.MUTED : EditorUi.WARNING, mouseX, mouseY);
        EditorUi.panel(graphics, PANE_X - 4, height - 47, paneWidth + 8, 43);
        label(graphics, text("camera_hint"), PANE_X + 4, height - 42, paneWidth - 8, EditorUi.MUTED, mouseX, mouseY);
        label(graphics, text("hint"), PANE_X + 4, height - 29, paneWidth - 8, EditorUi.MUTED, mouseX, mouseY);
        label(graphics, text("snapshot_hint"), PANE_X + 4, height - 16, paneWidth - 8, EditorUi.MUTED, mouseX, mouseY);
        super.render(graphics, mouseX, mouseY, partialTick);
        if (hoverText != null) graphics.renderTooltip(font, hoverText, mouseX, mouseY);
    }

    private void renderList(GuiGraphics graphics, List<LootCrateEdit> visible, int mouseX, int mouseY) {
        EditorUi.panel(graphics, PANE_X - 1, PANE_TOP - 1, paneWidth + 2, paneHeight + 2);
        scroll = Mth.clamp(scroll, 0, Math.max(0, visible.size() * ROW_HEIGHT - paneHeight));
        graphics.enableScissor(PANE_X, PANE_TOP, PANE_X + paneWidth, PANE_TOP + paneHeight);
        if (visible.isEmpty()) {
            int textY = PANE_TOP + Math.max(10, (paneHeight - 34) / 2);
            label(graphics, ui("empty"), PANE_X + 10, textY, paneWidth - 20, EditorUi.TEXT, mouseX, mouseY);
            label(graphics, ui("empty_hint"), PANE_X + 10, textY + 17, paneWidth - 20, EditorUi.MUTED, mouseX, mouseY);
        }
        for (int i = scroll / ROW_HEIGHT; i < visible.size(); i++) {
            int rowY = PANE_TOP + i * ROW_HEIGHT - scroll;
            if (rowY >= PANE_TOP + paneHeight) break;
            var entry = visible.get(i);
            int rowX = PANE_X + 3, rowWidth = paneWidth - 12;
            boolean selected = draft.selected(entry);
            boolean hovered = mouseX >= rowX && mouseX < rowX + rowWidth && mouseY >= rowY + 1
                    && mouseY < rowY + ROW_HEIGHT - 2 && inPane(mouseX, mouseY);
            graphics.fill(rowX, rowY + 1, rowX + rowWidth, rowY + ROW_HEIGHT - 2,
                    selected ? 0xe8244945 : hovered ? EditorUi.HOVER : EditorUi.SURFACE);
            if (selected) graphics.fill(rowX, rowY + 1, rowX + 2, rowY + ROW_HEIGHT - 2, EditorUi.ACCENT);
            if (selected) graphics.fill(rowX + 6, rowY + 7, rowX + 11, rowY + 12, EditorUi.ACCENT);
            else graphics.renderOutline(rowX + 6, rowY + 7, 5, 5, EditorUi.BORDER);
            Component state = state(entry);
            int stateWidth = rowWidth >= 190 ? font.width(state) + 10 : 0;
            EditorUi.text(graphics, font, Component.literal(entry.x() + ", " + entry.y() + ", " + entry.z()),
                    rowX + 17, rowY + 5, rowWidth - 24 - stateWidth, selected ? EditorUi.ACCENT : EditorUi.TEXT);
            if (stateWidth > 0) {
                int stateX = rowX + rowWidth - stateWidth - 4;
                graphics.fill(stateX, rowY + 3, stateX + stateWidth, rowY + 16, 0x70101c27);
                EditorUi.text(graphics, font, state, stateX + 5, rowY + 5, stateWidth - 10,
                        entry.enabled() ? entry.opened() ? EditorUi.WARNING : EditorUi.ACCENT : EditorUi.MUTED);
            }
            EditorUi.text(graphics, font, Component.literal(entry.table()), rowX + 17, rowY + 19, rowWidth - 24, EditorUi.MUTED);
            if (hovered) hoverText = crateTooltip(entry);
        }
        graphics.disableScissor();
        EditorUi.scrollbar(graphics, PANE_X + paneWidth - 3, PANE_TOP, paneHeight, visible.size() * ROW_HEIGHT, scroll);
    }

    private Component state(LootCrateEdit entry) {
        return ui(entry.enabled() ? entry.opened() ? "state_open" : "state_loot" : "state_normal");
    }

    private Component crateTooltip(LootCrateEdit entry) {
        return Component.literal(entry.x() + ", " + entry.y() + ", " + entry.z() + " | ")
                .append(state(entry)).append("\n" + entry.block() + "\n" + entry.table());
    }

    private void renderWorldSelection(GuiGraphics graphics, List<LootCrateEdit> visible, int mouseX, int mouseY) {
        graphics.enableScissor(PANE_X, PANE_TOP, PANE_X + paneWidth, PANE_TOP + paneHeight);
        for (var entry : visible) {
            var point = project(entry);
            if (!visiblePoint(point)) continue;
            int x = (int) point.x(), y = (int) point.y();
            boolean selected = draft.selected(entry);
            graphics.fill(x - 4, y - 4, x + 5, y + 5, selected ? 0x5553c6a8 : 0x40331f10);
            graphics.renderOutline(x - 4, y - 4, 9, 9, selected ? EditorUi.ACCENT : EditorUi.WARNING);
            if (inPane(mouseX, mouseY) && Math.abs(mouseX - x) <= 5 && Math.abs(mouseY - y) <= 5) hoverText = crateTooltip(entry);
        }
        if (dragging) {
            int x = (int) Math.min(startX, endX), y = (int) Math.min(startY, endY);
            int w = (int) Math.abs(endX - startX) + 1, h = (int) Math.abs(endY - startY) + 1;
            graphics.fill(x, y, x + w, y + h, 0x2854d3b7);
            graphics.renderOutline(x, y, w, h, EditorUi.ACCENT);
        }
        graphics.disableScissor();
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        if (saving) return true;
        if (x >= right) LootEditorCamera.clearMovement();
        if (button == 0 && inContent(x, y) && x >= width - 9 && CONTENT_HEIGHT > contentBottom - contentTop) {
            draggingContentScroll = true;
            scrollContentTo(y);
            return true;
        }
        if (button == 1 && !list && inPane(x, y)) {
            setFocused(null);
            rotating = true;
            return true;
        }
        if (button == 0 && inPane(x, y)) {
            setFocused(null);
            if (list) {
                var entries = draft.visible();
                if (x >= PANE_X + paneWidth - 8) {
                    if (entries.size() * ROW_HEIGHT > paneHeight) {
                        draggingListScroll = true;
                        scrollListTo(y);
                    }
                } else if (x >= PANE_X + 3 && x < PANE_X + paneWidth - 9) {
                    int index = (int) (y - PANE_TOP + scroll) / ROW_HEIGHT;
                    int rowOffset = (int) (y - PANE_TOP + scroll) % ROW_HEIGHT;
                    if (index >= 0 && index < entries.size() && rowOffset >= 1 && rowOffset < ROW_HEIGHT - 2)
                        draft.toggle(entries.get(index));
                }
            } else {
                dragging = true;
                startX = endX = x;
                startY = endY = y;
                add = hasShiftDown();
            }
            return true;
        }
        // Keep clipped controls in keyboard focus order, without allowing clicks through the fixed chrome.
        for (var child : List.copyOf(children())) {
            if (child instanceof AbstractWidget widget && contentPositions.containsKey(widget) && !inContent(x, y)) continue;
            if (child.mouseClicked(x, y, button)) {
                // Switching views rebuilds the widgets during the callback; never refocus the removed button.
                if (minecraft.screen == this && children().contains(child)) {
                    setFocused(child);
                    if (button == 0) setDragging(true);
                }
                return true;
            }
        }
        return false;
    }

    private void scrollContentTo(double y) {
        contentScroll = (int) ((y - contentTop) / (contentBottom - contentTop) * Math.max(0, CONTENT_HEIGHT - contentBottom + contentTop));
        positionContent();
    }

    private void scrollListTo(double y) {
        scroll = Mth.clamp((int) ((y - PANE_TOP) / paneHeight * Math.max(0, draft.visible().size() * ROW_HEIGHT - paneHeight)),
                0, Math.max(0, draft.visible().size() * ROW_HEIGHT - paneHeight));
    }

    @Override
    public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
        if (saving) return true;
        if (draggingContentScroll) { scrollContentTo(y); return true; }
        if (draggingListScroll) { scrollListTo(y); return true; }
        if (button == 1 && rotating) { LootEditorCamera.turn(dx, dy); return true; }
        if (dragging) {
            endX = Math.max(PANE_X, Math.min(PANE_X + paneWidth, x));
            endY = Math.max(PANE_TOP, Math.min(PANE_TOP + paneHeight, y));
            return true;
        }
        return super.mouseDragged(x, y, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double x, double y, int button) {
        if (button == 1) rotating = false;
        if (draggingContentScroll || draggingListScroll) {
            draggingContentScroll = draggingListScroll = false;
            return true;
        }
        if (dragging && button == 0) {
            dragging = false;
            if (Math.hypot(endX - startX, endY - startY) < 4) {
                var camera = minecraft.gameRenderer.getMainCamera();
                var origin = camera.getPosition();
                var end = origin.add(LootEditorProjection.ray(camera.rotation(), minecraft.options.fov().get(),
                        width, height, startX, startY).scale(4096));
                var hit = draft.visible().stream().filter(entry -> LootWorldEditor.bounds(entry)
                        .clip(origin, end).isPresent()).min(java.util.Comparator.comparingDouble(entry ->
                        LootWorldEditor.bounds(entry).clip(origin, end).orElseThrow().distanceToSqr(origin)));
                if (hit.isPresent()) draft.toggle(hit.get());
                else draft.visible().stream().filter(entry -> {
                    var point = project(entry);
                    return visiblePoint(point) && Math.hypot(point.x() - startX, point.y() - startY) <= 8;
                }).min(java.util.Comparator.comparingDouble(entry -> project(entry).depth())).ifPresent(draft::toggle);
            } else {
                var positions = draft.visible().stream().filter(entry -> {
                    var point = project(entry);
                    return visiblePoint(point) && point.x() >= Math.min(startX, endX) && point.x() <= Math.max(startX, endX)
                            && point.y() >= Math.min(startY, endY) && point.y() <= Math.max(startY, endY);
                }).map(LootCrateEdit::position).collect(java.util.stream.Collectors.toSet());
                draft.selectPositions(positions, add);
            }
            return true;
        }
        return super.mouseReleased(x, y, button);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double delta) {
        if (saving) return true;
        if (inContent(x, y)) {
            LootEditorCamera.clearMovement();
            contentScroll -= (int) Math.round(delta * 22);
            positionContent();
            return true;
        }
        if (inPane(x, y)) {
            if (list) scroll = Mth.clamp(scroll - (int) Math.round(delta * ROW_HEIGHT), 0, Math.max(0, draft.visible().size() * ROW_HEIGHT - paneHeight));
            else LootEditorCamera.zoom(delta);
            return true;
        }
        return super.mouseScrolled(x, y, delta);
    }

    @Override
    public void onClose() {
        if (saving) return;
        if (draft.dirty()) minecraft.setScreen(new ConfirmScreen(confirmed -> {
            if (confirmed) finish();
            else minecraft.setScreen(this);
        }, text("discard"), text("discard_hint")));
        else finish();
    }

    private void finish() { LootWorldEditor.stop(); minecraft.setScreen(null); }

    @Override
    public void removed() {
        rememberOptions();
        LootEditorCamera.clearMovement();
        super.removed();
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        if (saving) return true;
        if (key == GLFW.GLFW_KEY_F6) { LootEditorCamera.focus(); return true; }
        if (LootEditorCamera.key(key, scan, GLFW.GLFW_PRESS)) return true;
        boolean handled = super.keyPressed(key, scan, modifiers);
        revealFocusedContent();
        return handled;
    }

    @Override
    public boolean keyReleased(int key, int scan, int modifiers) {
        LootEditorCamera.key(key, scan, GLFW.GLFW_RELEASE);
        return super.keyReleased(key, scan, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (saving) return true;
        revealFocusedContent();
        return super.charTyped(codePoint, modifiers);
    }

    @Override public boolean isPauseScreen() { return false; }
}
