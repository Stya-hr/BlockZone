package dev.stya.blockzone.client.editor;

import dev.stya.blockzone.editor.loot.LootCrateDraft;
import dev.stya.blockzone.editor.loot.LootCrateEdit;
import dev.stya.blockzone.editor.loot.LootEditorProjection;
import dev.stya.blockzone.net.battlezone.BattlezoneNetwork;
import dev.stya.blockzone.net.editor.*;
import dev.stya.blockzone.net.editor.LootEditorResultS2CPacket;
import dev.stya.blockzone.net.editor.LootEditorS2CPacket;
import dev.stya.blockzone.net.editor.SaveLootEditorC2SPacket;
import dev.stya.blockzone.zone.ZoneGeometry;
import dev.stya.blockzone.zone.ZoneShape;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class LootCrateEditorScreen extends Screen {
    private final LootEditorS2CPacket packet;
    private final LootCrateDraft draft;
    private EditBox table, seed;
    private Checkbox close, enabled;
    private Button save;
    private String filterValue = "";
    private String tableValue = "blockzone:chests/common", seedValue = "0";
    String message = "";
    private boolean closeValue = true, enabledValue, list, saving, dragging;
    private int right, paneWidth, paneHeight, scroll;
    private double startX, startY, endX, endY;
    private boolean add;
    public static void open(LootEditorS2CPacket packet) {
        var mc = Minecraft.getInstance();
        if (mc.level == null || !mc.level.dimension().location().equals(packet.dimension())) return;
        var screen = new LootCrateEditorScreen(packet);
        if (!LootWorldEditor.start(screen)) screen.message = Component.translatable("editor.blockzone.camera_busy").getString();
        mc.setScreen(screen);
    }
    public static void result(LootEditorResultS2CPacket packet) {
        if (Minecraft.getInstance().screen instanceof LootCrateEditorScreen screen && screen.packet.token().equals(packet.token())) {
            screen.saving = false; screen.message = Component.translatable(packet.messageKey()).getString();
            if (packet.saved()) screen.draft.markSaved();
        }
    }
    private LootCrateEditorScreen(LootEditorS2CPacket packet) {
        super(Component.translatable("editor.blockzone.loot.title", packet.mapName()));
        this.packet = packet; draft = new LootCrateDraft(packet.crates());
    }
    private Component text(String key) { return Component.translatable("editor.blockzone.loot." + key); }
    private Button button(int x, int y, int w, String key, Runnable action) {
        return addRenderableWidget(Button.builder(text(key), b -> { if (!saving) action.run(); }).bounds(x, y, w, 20).build());
    }
    @Override protected void init() {
        right = width - 202; paneWidth = right - 24; paneHeight = height - 124;
        var filter = addRenderableWidget(new EditBox(font, 12, 28, paneWidth, 18, text("filter")));
        filter.setValue(filterValue);
        filter.setHint(text("filter")); filter.setResponder(value -> { filterValue = value; draft.filter(value); scroll = 0; });
        button(12, 52, 80, "overview", () -> list = false);
        button(96, 52, 80, "list", () -> list = true);
        table = addRenderableWidget(new EditBox(font, right, 42, 156, 18, text("table")));
        table.setMaxLength(256); table.setValue(tableValue); table.setResponder(value -> tableValue = value);
        button(right + 160, 41, 30, "choose", () -> { closeValue = close.selected(); enabledValue=enabled.selected(); minecraft.setScreen(new LootTablePickerScreen(this, packet.tables())); });
        seed = addRenderableWidget(new EditBox(font, right, 78, 190, 18, text("seed")));
        seed.setMaxLength(20); seed.setValue(seedValue); seed.setResponder(value -> seedValue = value);
        enabled=addRenderableWidget(new Checkbox(right,102,190,20,text("enabled"),enabledValue));
        close = addRenderableWidget(new Checkbox(right, 124, 190, 20, text("close"), closeValue));
        button(right, 146, 190, "apply", () -> {
            try {
                if (!packet.tables().contains(tableValue.strip())) throw new IllegalArgumentException(text("invalid_table").getString());
                draft.apply(tableValue, Long.parseLong(seedValue.strip()), close.selected(), enabled.selected());
                closeValue = close.selected(); enabledValue=enabled.selected(); message = text("draft_applied").getString();
            } catch (NumberFormatException failure) { message = text("invalid_seed").getString(); }
            catch (IllegalArgumentException failure) { message = failure.getMessage(); }
        });
        button(right, 172, 92, "all", draft::selectAll);
        button(right + 98, 172, 92, "clear", draft::clearSelection);
        save = button(right, height - 29, 92, "save", () -> {
            if (!draft.dirty()) return;
            saving = true; message = text("saving").getString();
            BattlezoneNetwork.saveLootEditor(new SaveLootEditorC2SPacket(packet.token(), draft.changes()));
        });
        button(right + 98, height - 29, 92, "exit", this::onClose);
    }
    void pick(String value) { tableValue = value; minecraft.setScreen(this); }
    @Override public void tick() { table.tick(); seed.tick(); save.active = !saving && draft.dirty(); }
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
        double height = Math.max(mapTop() + 24, y + radius + Math.max(32, radius * 1.25));
        var mc = Minecraft.getInstance();
        double w = mc.getWindow().getGuiScaledWidth(), h = mc.getWindow().getGuiScaledHeight();
        // Place the focus under the world viewport, leaving room for the parameter panel.
        double offset = (height - y) * Math.tan(Math.toRadians(mc.options.fov().get()) / 2) * w / h * (226 / w);
        return new ZoneGeometry((minX + maxX) / 2 + .5 + offset, y,
                (minZ + maxZ) / 2 + .5, radius, ZoneShape.CYLINDER);
    }
    private LootEditorProjection.Point project(LootCrateEdit entry) {
        var camera = minecraft.gameRenderer.getMainCamera();
        return LootEditorProjection.project(new net.minecraft.world.phys.Vec3(entry.x() + .5, entry.y() + .5, entry.z() + .5),
                camera.getPosition(), camera.rotation(), minecraft.options.fov().get(), width, height);
    }
    private boolean visiblePoint(LootEditorProjection.Point point) {
        return point != null && inPane(point.x(), point.y());
    }
    private boolean inPane(double x, double y) { return x >= 12 && x <= 12 + paneWidth && y >= 80 && y <= 80 + paneHeight; }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(right - 8, 0, width, height, 0xe0182028);
        graphics.fill(4, 4, right - 12, 76, 0xa0182028);
        graphics.drawCenteredString(font, title, width / 2, 8, 0xffffff);
        if (list) graphics.fill(12, 80, 12 + paneWidth, 80 + paneHeight, 0xd0182028);
        var visible = draft.visible();
        if (list) {
            int rows = Math.max(1, paneHeight / 25);
            scroll = Math.max(0, Math.min(scroll, Math.max(0, visible.size() - rows)));
            for (int i = scroll; i < Math.min(visible.size(), scroll + rows); i++) {
                var entry = visible.get(i); int y = 80 + (i - scroll) * 25;
                if (draft.selected(entry)) graphics.fill(12, y, 12 + paneWidth, y + 24, 0xff355d46);
                graphics.drawString(font, entry.x() + ", " + entry.y() + ", " + entry.z() + (entry.enabled() ? (entry.opened()?" [open]":" [loot]"):" [normal]"), 16, y + 2, 0xffffff);
                graphics.drawString(font, font.plainSubstrByWidth(entry.table(), paneWidth - 8), 16, y + 13, 0xb0b8c0);
            }
        } else {
            for (var entry : visible) {
                var point = project(entry);
                if (!visiblePoint(point)) continue;
                int x = (int)point.x(), y = (int)point.y();
                graphics.renderOutline(x - 4, y - 4, 9, 9, draft.selected(entry) ? 0xff65e096 : 0xffd2ac61);
                if (Math.abs(mouseX - x) <= 5 && Math.abs(mouseY - y) <= 5)
                    graphics.renderTooltip(font, Component.literal(entry.x() + ", " + entry.y() + ", " + entry.z() + " | " + entry.block() + " | " + entry.table()), mouseX, mouseY);
            }
            if (dragging) graphics.renderOutline((int)Math.min(startX, endX), (int)Math.min(startY, endY),
                    (int)Math.abs(endX - startX) + 1, (int)Math.abs(endY - startY) + 1, 0xff65e096);
        }
        graphics.drawString(font, text("table"), right, 30, 0xffffff);
        graphics.drawString(font, text("seed"), right, 66, 0xffffff);
        graphics.drawString(font, Component.translatable("editor.blockzone.loot.selected", draft.selectedCount(), draft.entries().size()), right, 202, 0xffffff);
        graphics.drawString(font, Component.translatable("editor.blockzone.loot.changed", draft.changes().size()), right, 214, 0xffffff);
        graphics.drawString(font,font.plainSubstrByWidth(message,190),right,226,0xffd2ac61);
        graphics.fill(4, height - 46, right - 12, height, 0xa0182028);
        graphics.drawString(font, text("camera_hint"), 12, height - 45, 0xb0b8c0);
        graphics.drawString(font, text("hint"), 12, height - 32, 0xb0b8c0);
        graphics.drawString(font, text("snapshot_hint"), 12, height - 19, 0xb0b8c0);
        super.render(graphics, mouseX, mouseY, partialTick);
    }
    @Override public boolean mouseClicked(double x, double y, int button) {
        if (saving) return true;
        if (x >= right - 8) LootEditorCamera.clearMovement();
        if (button == 1 && !list && inPane(x, y)) { setFocused(null); return true; }
        if (button == 0 && inPane(x, y)) {
            setFocused(null);
            if (list) {
                var entries = draft.visible(); int index = scroll + (int)((y - 80) / 25);
                if (index < entries.size()) draft.toggle(entries.get(index));
            } else { dragging = true; startX = endX = x; startY = endY = y; add = hasShiftDown(); }
            return true;
        }
        return super.mouseClicked(x, y, button);
    }
    @Override public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
        if (button == 1 && !list && x < right - 8) { LootEditorCamera.turn(dx, dy); return true; }
        if (dragging) { endX = Math.max(12, Math.min(12 + paneWidth, x)); endY = Math.max(80, Math.min(80 + paneHeight, y)); return true; }
        return super.mouseDragged(x, y, button, dx, dy);
    }
    @Override public boolean mouseReleased(double x, double y, int button) {
        if (dragging) {
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
    @Override public boolean mouseScrolled(double x, double y, double delta) {
        if (inPane(x, y)) { if (list) scroll -= (int)delta; else LootEditorCamera.zoom(delta); return true; }
        return super.mouseScrolled(x, y, delta);
    }
    @Override public void onClose() {
        if (saving) return;
        if (draft.dirty()) minecraft.setScreen(new ConfirmScreen(confirmed -> { if (confirmed) finish(); else minecraft.setScreen(this); }, text("discard"), text("discard_hint")));
        else finish();
    }
    private void finish() { LootWorldEditor.stop(); minecraft.setScreen(null); }
    @Override public void removed() { LootEditorCamera.clearMovement(); super.removed(); }
    @Override public boolean keyPressed(int key, int scan, int modifiers) {
        if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_F6) { LootEditorCamera.focus(); return true; }
        if (LootEditorCamera.key(key, scan, org.lwjgl.glfw.GLFW.GLFW_PRESS)) return true;
        return super.keyPressed(key, scan, modifiers);
    }
    @Override public boolean keyReleased(int key, int scan, int modifiers) {
        LootEditorCamera.key(key, scan, org.lwjgl.glfw.GLFW.GLFW_RELEASE);
        return super.keyReleased(key, scan, modifiers);
    }
    @Override public boolean isPauseScreen() { return false; }
}
