package dev.stya.blockzone.client.editor;

import dev.stya.blockzone.editor.zone.PoisonEditorGeometry;
import dev.stya.blockzone.zone.PoisonPath;
import dev.stya.blockzone.zone.ZoneGeometry;
import dev.stya.blockzone.zone.ZoneShape;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

/** Transparent parameter panel with direct manipulation of the visible world. */
public final class PoisonEditorScreen extends ModernEditorScreen {
    private static final String[] FIELD_KEYS = {"x", "z", "radius", "waitSeconds", "shrinkSeconds", "damageMultiplier"};
    private static String fieldKey(int index) { return "setting.battlezone.poisonCircle." + FIELD_KEYS[index]; }
    private final List<String> fields = new ArrayList<>();
    private volatile PanelState panelState;
    private record PanelState(String sequence, String circle, String shape, List<String> values) {}
    private int panel;
    private boolean moving;
    public PoisonEditorScreen() {
        super(Component.translatable("editor.blockzone.title"));
        syncFields();
    }
    @Override protected void init() { if (PoisonWorldEditor.draft != null) snapshot(); super.init(); }
    private void syncFields() {
        if (PoisonWorldEditor.draft == null) return;
        var c = PoisonWorldEditor.draft.selected();
        fields.clear();
        fields.addAll(List.of(Double.toString(c.x()), Double.toString(c.z()), Double.toString(c.radius()),
                Integer.toString(c.waitSeconds()), Integer.toString(c.shrinkSeconds()), Double.toString(c.damageMultiplier())));
        snapshot();
    }
    private void snapshot() {
        var d = PoisonWorldEditor.draft;
        panelState = new PanelState(EditorPanel.tr("editor.blockzone.sequence", d.sequence()+1, d.paths().size()),
                EditorPanel.tr("editor.blockzone.circle", d.circle()+1, d.path().circles().size()),
                EditorPanel.tr("setting.battlezone.poisonShape." + d.path().shape().id()), List.copyOf(fields));
    }
    @Override void buildPanel(EditorPanel ui) {
        var state = panelState;
        if (state == null) return;
        var body = ui.body();
        ui.section("target");
        var sequences = ui.row(body);
        ui.button(sequences, "‹", () -> change(() -> PoisonWorldEditor.draft.selectSequence(PoisonWorldEditor.draft.sequence()-1)));
        ui.label(sequences, state.sequence(), 12).setLayoutParams(new icyllis.modernui.widget.LinearLayout.LayoutParams(0, -2, 3));
        ui.button(sequences, "›", () -> change(() -> PoisonWorldEditor.draft.selectSequence(PoisonWorldEditor.draft.sequence()+1)));
        var sequenceActions = ui.row(body);
        ui.button(sequenceActions, EditorPanel.tr("editor.blockzone.copy_sequence"), () -> change(PoisonWorldEditor.draft::duplicateSequence));
        ui.danger(ui.button(sequenceActions, EditorPanel.tr("editor.blockzone.delete_sequence"), () -> change(PoisonWorldEditor.draft::removeSequence)));
        var circles = ui.row(body);
        ui.button(circles, "‹", () -> change(() -> PoisonWorldEditor.draft.selectCircle(PoisonWorldEditor.draft.circle()-1)));
        ui.label(circles, state.circle(), 12).setLayoutParams(new icyllis.modernui.widget.LinearLayout.LayoutParams(0, -2, 3));
        ui.button(circles, "›", () -> change(() -> PoisonWorldEditor.draft.selectCircle(PoisonWorldEditor.draft.circle()+1)));
        var circleActions = ui.row(body);
        ui.button(circleActions, EditorPanel.tr("editor.blockzone.copy_circle"), () -> change(PoisonWorldEditor.draft::addCircle));
        ui.danger(ui.button(circleActions, EditorPanel.tr("editor.blockzone.delete_circle"), () -> change(PoisonWorldEditor.draft::removeCircle)));
        for (int[] group : new int[][]{{0, 1}, {2}, {3, 4}, {5}}) {
            if (group[0] == 0) ui.section("geometry");
            if (group[0] == 3) ui.section("timing");
            var row = ui.row(body);
            for (int index : group) {
                ui.field(row, EditorPanel.tr(fieldKey(index)), state.values().get(index), 32, value -> {
                    if (PoisonWorldEditor.saving) return;
                    fields.set(index, value); snapshot(); apply();
                }).setTooltipText(EditorPanel.tr(fieldKey(index) + ".desc"));
            }
        }
        ui.section("arrangement");
        var order = ui.row(body);
        ui.button(order, EditorPanel.tr("editor.blockzone.move_up"), () -> change(() -> PoisonWorldEditor.draft.moveCircle(-1)));
        ui.button(order, EditorPanel.tr("editor.blockzone.move_down"), () -> change(() -> PoisonWorldEditor.draft.moveCircle(1)));
        ui.button(ui.row(body), state.shape(), () -> change(() -> PoisonWorldEditor.draft.setShape(ZoneShape.values()[
                (PoisonWorldEditor.draft.path().shape().ordinal()+1) % ZoneShape.values().length])));
        ui.button(ui.row(body), EditorPanel.tr("editor.blockzone.observe"), () -> { if (apply()) minecraft.setScreen(null); });
        ui.help("editor.blockzone.drag_controls", "editor.blockzone.colors", "editor.blockzone.camera_controls");
    }
    @Override void save() { if (apply()) PoisonWorldEditor.save(); }
    @Override void updatePanel(EditorPanel ui) {
        if (PoisonWorldEditor.draft == null) return;
        String warning = PoisonWorldEditor.message;
        var c = PoisonWorldEditor.draft.selected();
        try {
            var actual = new ZoneGeometry(c.x(), PoisonWorldEditor.y(), c.z(), c.radius(), PoisonWorldEditor.draft.path().shape()).fitInside(PoisonWorldEditor.bounds());
            if (warning.isEmpty() && (actual.centerX()!=c.x() || actual.centerZ()!=c.z()))
                warning = EditorPanel.tr("editor.blockzone.adjusted", String.format(java.util.Locale.ROOT, "%.2f", actual.centerX()),
                        String.format(java.util.Locale.ROOT, "%.2f", actual.centerZ()));
        } catch (IllegalArgumentException bad) { warning = EditorPanel.tr("editor.blockzone.too_large"); }
        ui.status("", warning, PoisonWorldEditor.dirty(), PoisonWorldEditor.saving);
    }
    private void change(Runnable action) {
        if (!apply()) return;
        try { action.run(); syncFields(); refreshPanel(); }
        catch (IllegalArgumentException invalid) { PoisonWorldEditor.message = invalid.getMessage(); }
    }
    private boolean apply() {
        if (fields.size() != 6 || PoisonWorldEditor.saving) return false;
        try {
            double x = number(0), z = number(1), radius = number(2), damage = number(5);
            int wait = Integer.parseInt(fields.get(3)), transition = Integer.parseInt(fields.get(4));
            if (radius < 0 || radius > 30000000 || wait < 0 || transition < 0 || wait > 1000000
                    || transition > 1000000 || damage < 0 || damage > Float.MAX_VALUE) throw new IllegalArgumentException();
            PoisonWorldEditor.draft.replace(new PoisonPath.Circle(x, z, radius, wait, transition, damage));
            PoisonWorldEditor.message = ""; return true;
        } catch (RuntimeException invalid) {
            PoisonWorldEditor.message = Component.translatable("editor.blockzone.invalid").getString(); return false;
        }
    }
    private double number(int index) {
        double value = Double.parseDouble(fields.get(index));
        if (!Double.isFinite(value)) throw new IllegalArgumentException(); return value;
    }
    @Override public void onClose() {
        if (PoisonWorldEditor.saving) return;
        boolean unapplied = false;
        var c = PoisonWorldEditor.draft == null ? null : PoisonWorldEditor.draft.selected();
        if (c != null && fields.size() == 6) {
            String[] values = {Double.toString(c.x()), Double.toString(c.z()), Double.toString(c.radius()),
                    Integer.toString(c.waitSeconds()), Integer.toString(c.shrinkSeconds()), Double.toString(c.damageMultiplier())};
            for (int i = 0; i < 6; i++) if (!values[i].equals(fields.get(i))) unapplied = true;
        }
        if (PoisonWorldEditor.dirty() || unapplied) {
            minecraft.setScreen(EditorConfirmScreen.create(yes -> { if (yes) PoisonWorldEditor.discard(); else minecraft.setScreen(this); },
                    Component.translatable("editor.blockzone.discard"), Component.translatable("editor.blockzone.discard_hint")));
        } else PoisonWorldEditor.discard();
    }
    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        panel = panelLeft();
        super.render(g, mouseX, mouseY, partialTick);
    }
    private Vec3 ground(double mouseX, double mouseY) {
        var camera = minecraft.gameRenderer.getMainCamera();
        return PoisonEditorGeometry.ground(camera.getPosition(), camera.rotation(),
                minecraft.options.fov().get(), (double) width / height, 2*mouseX/width-1, 1-2*mouseY/height,
                PoisonWorldEditor.y());
    }
    private void manipulate(double x, double y) {
        var point = ground(x,y);
        if (point == null) { PoisonWorldEditor.message = Component.translatable("editor.blockzone.look_down").getString(); return; }
        var c = PoisonWorldEditor.draft.selected();
        PoisonWorldEditor.setGeometry(point.x, point.z, c.radius());
        syncFields(); fragment.syncValues(List.copyOf(fields));
    }
    @Override public boolean keyPressed(int key, int scan, int modifiers) {
        if (key == GLFW.GLFW_KEY_F6) { PoisonEditorCamera.focus(); return true; }
        if (PoisonEditorCamera.key(key, scan, GLFW.GLFW_PRESS)) return true;
        return super.keyPressed(key, scan, modifiers);
    }
    @Override public boolean keyReleased(int key, int scan, int modifiers) {
        PoisonEditorCamera.key(key, scan, GLFW.GLFW_RELEASE);
        return super.keyReleased(key, scan, modifiers);
    }
    @Override public void removed() { PoisonEditorCamera.clearMovement(); super.removed(); }
    @Override public boolean mouseClicked(double x, double y, int button) {
        if (x >= panel) { PoisonEditorCamera.clearMovement(); return super.mouseClicked(x,y,button); }
        clearTextFocus();
        if (PoisonWorldEditor.saving) return true;
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && apply()) {
            moving = true; manipulate(x,y); return true;
        }
        return button == GLFW.GLFW_MOUSE_BUTTON_RIGHT || super.mouseClicked(x,y,button);
    }
    @Override public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
        if (x < panel && !PoisonWorldEditor.saving) {
            if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT && minecraft.player != null) {
                PoisonEditorCamera.turn(dx,dy); return true;
            }
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && moving) { manipulate(x,y); return true; }
        }
        return super.mouseDragged(x,y,button,dx,dy);
    }
    @Override public boolean mouseReleased(double x, double y, int button) { moving=false; return super.mouseReleased(x,y,button); }
    @Override public boolean mouseScrolled(double x, double y, double delta) {
        if (x < panel && apply()) {
            var c = PoisonWorldEditor.draft.selected();
            PoisonWorldEditor.setGeometry(c.x(),c.z(),c.radius()+delta*(hasControlDown()?10:1));
            syncFields(); fragment.syncValues(List.copyOf(fields)); return true;
        }
        return super.mouseScrolled(x,y,delta);
    }
}
