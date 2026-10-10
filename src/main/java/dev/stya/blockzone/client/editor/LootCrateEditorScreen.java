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
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.network.chat.Component;

public final class LootCrateEditorScreen extends ModernEditorScreen {
    private final LootEditorS2CPacket packet;
    private final LootCrateDraft draft;
    private String filterValue = "";
    private String tableValue = "blockzone:chests/common", seedValue = "0";
    String message = "";
    private boolean closeValue = true, enabledValue, list, saving, dragging;
    private int right, paneWidth, paneHeight;
    private volatile PanelState panelState;
    private record PanelState(String table, String seed, String filter, boolean close, boolean enabled) {}
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
        this.packet = packet; draft = new LootCrateDraft(packet.crates()); snapshot();
    }
    private Component text(String key) { return Component.translatable("editor.blockzone.loot." + key); }
    private void snapshot() { panelState = new PanelState(tableValue, seedValue, filterValue, closeValue, enabledValue); }
    @Override void buildPanel(EditorPanel ui) {
        var state = panelState;
        var body = ui.body();
        ui.section("target");
        ui.field(ui.row(body), text("filter").getString(), state.filter(), 256, value -> {
            if (saving) return;
            filterValue = value; draft.filter(value); snapshot();
        });
        var views = ui.row(body);
        ui.button(views, text("overview").getString(), () -> list = false);
        ui.button(views, text("list").getString(), () -> list = true);
        var selection = ui.row(body);
        ui.button(selection, text("all").getString(), () -> { if (!saving) draft.selectAll(); });
        ui.button(selection, text("clear").getString(), () -> { if (!saving) draft.clearSelection(); });
        ui.section("loot_settings");
        ui.field(ui.row(body), text("table").getString(), state.table(), 256, value -> { if (!saving) { tableValue = value; snapshot(); } });
        ui.button(ui.row(body), text("choose").getString(), () -> {
            if (!saving) { snapshot(); minecraft.setScreen(new LootTablePickerScreen(this, packet.tables())); }
        });
        ui.field(ui.row(body), text("seed").getString(), state.seed(), 20, value -> { if (!saving) { seedValue = value; snapshot(); } });
        ui.check(text("enabled").getString(), state.enabled(), value -> { if (!saving) { enabledValue = value; snapshot(); } });
        ui.check(text("close").getString(), state.close(), value -> { if (!saving) { closeValue = value; snapshot(); } });
        ui.button(ui.row(body), text("apply").getString(), this::apply);
        ui.help("editor.blockzone.loot.camera_hint", "editor.blockzone.loot.hint", "editor.blockzone.loot.snapshot_hint");
    }
    private void apply() {
        if (saving) return;
        try {
            if (!packet.tables().contains(tableValue.strip())) throw new IllegalArgumentException(text("invalid_table").getString());
            draft.apply(tableValue, Long.parseLong(seedValue.strip()), closeValue, enabledValue);
            message = text("draft_applied").getString();
        } catch (NumberFormatException failure) { message = text("invalid_seed").getString(); }
        catch (IllegalArgumentException failure) { message = failure.getMessage(); }
    }
    @Override void save() {
        if (saving || !draft.dirty()) return;
        saving = true; message = text("saving").getString();
        BattlezoneNetwork.saveLootEditor(new SaveLootEditorC2SPacket(packet.token(), draft.changes()));
    }
    @Override void updatePanel(EditorPanel ui) {
        ui.showList(list ? draft.visible().stream().map(entry -> new EditorPanel.ListRow(
                entry.x() + ", " + entry.y() + ", " + entry.z() + " · "
                        + EditorPanel.tr("editor.blockzone.ui." + (!entry.enabled() ? "normal" : entry.opened() ? "opened" : "loot"))
                        + "\n" + entry.table(), draft.selected(entry), () -> { if (!saving) draft.toggle(entry); })).toList() : java.util.List.of(), list);
        ui.status(EditorPanel.tr("editor.blockzone.loot.selected", draft.selectedCount(), draft.entries().size())
                + " · " + EditorPanel.tr("editor.blockzone.loot.changed", draft.changes().size())
                + "\n" + message, draft.dirty(), saving);
    }
    void pick(String value) { tableValue = value; snapshot(); minecraft.setScreen(this); }
    @Override protected void init() { snapshot(); super.init(); }
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
        double offset = (height - y) * Math.tan(Math.toRadians(mc.options.fov().get()) / 2) * w / h * (panelFraction + 12 / w);
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
    private boolean inPane(double x, double y) { return x >= 12 && x <= 12 + paneWidth && y >= 12 && y <= 12 + paneHeight; }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        right = panelLeft(); paneWidth = Math.max(0, right - 24); paneHeight = Math.max(0, height - 24);
        var visible = draft.visible();
        if (!list) {
            for (var entry : visible) {
                var point = project(entry);
                if (!visiblePoint(point)) continue;
                int x = (int)point.x(), y = (int)point.y();
                graphics.renderOutline(x - 4, y - 4, 9, 9, draft.selected(entry) ? 0xff65e096 : 0xffd2ac61);
                if (Math.abs(mouseX - x) <= 5 && Math.abs(mouseY - y) <= 5)
                    graphics.renderComponentTooltip(font, java.util.List.of(
                            Component.literal(entry.x() + ", " + entry.y() + ", " + entry.z()),
                            Component.literal(entry.block()),
                            Component.literal(entry.table())), mouseX, mouseY);
            }
            if (dragging) graphics.renderOutline((int)Math.min(startX, endX), (int)Math.min(startY, endY),
                    (int)Math.abs(endX - startX) + 1, (int)Math.abs(endY - startY) + 1, 0xff65e096);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }
    @Override public boolean mouseClicked(double x, double y, int button) {
        if (saving) return true;
        if (x >= right - 8) LootEditorCamera.clearMovement();
        if (button == 1 && !list && inPane(x, y)) { clearTextFocus(); return true; }
        if (button == 0 && inPane(x, y)) {
            clearTextFocus();
            if (list) return super.mouseClicked(x, y, button);
            dragging = true; startX = endX = x; startY = endY = y; add = hasShiftDown();
            return true;
        }
        return super.mouseClicked(x, y, button);
    }
    @Override public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
        if (!saving && button == 1 && !list && inPane(x, y)) { LootEditorCamera.turn(dx, dy); return true; }
        if (dragging) { endX = Math.max(12, Math.min(12 + paneWidth, x)); endY = Math.max(12, Math.min(12 + paneHeight, y)); return true; }
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
        if (!saving && !list && inPane(x, y)) { LootEditorCamera.zoom(delta); return true; }
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
