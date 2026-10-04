package dev.stya.blockzone.client.editor;

import dev.stya.blockzone.util.battlezone.PoisonPath;
import dev.stya.blockzone.util.battlezone.ZoneGeometry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;
import java.util.ArrayList;
import java.util.List;

/** Transparent parameter panel with direct manipulation of the visible world. */
public final class PoisonEditorScreen extends Screen {
    private static final String[] FIELD_KEYS = {"x", "z", "radius", "waitSeconds", "shrinkSeconds", "damageMultiplier"};
    private static String fieldKey(int index) { return "setting.battlezone.poisonCircle." + FIELD_KEYS[index]; }
    private final List<EditBox> fields = new ArrayList<>();
    private int panel, panelScroll;
    private boolean moving, resizing;
    public PoisonEditorScreen() { super(Component.translatable("editor.blockzone.title")); }
    @Override public boolean isPauseScreen() { return false; }
    @Override protected void init() {
        if (PoisonWorldEditor.draft == null) { onClose(); return; }
        panel = width - 218; fields.clear();
        var draft = PoisonWorldEditor.draft;
        button(panel + 8, 27, 30, "<", () -> change(() -> draft.selectSequence(draft.sequence()-1)));
        button(panel + 178, 27, 30, ">", () -> change(() -> draft.selectSequence(draft.sequence()+1)));
        button(panel + 8, 51, 97, "editor.blockzone.copy_sequence", () -> change(draft::duplicateSequence));
        button(panel + 111, 51, 97, "editor.blockzone.delete_sequence", () -> change(draft::removeSequence));
        button(panel + 8, 77, 30, "<", () -> change(() -> draft.selectCircle(draft.circle()-1)));
        button(panel + 178, 77, 30, ">", () -> change(() -> draft.selectCircle(draft.circle()+1)));
        button(panel + 8, 101, 97, "editor.blockzone.copy_circle", () -> change(draft::addCircle));
        button(panel + 111, 101, 97, "editor.blockzone.delete_circle", () -> change(draft::removeCircle));
        var c = draft.selected();
        String[] values = {Double.toString(c.x()), Double.toString(c.z()), Double.toString(c.radius()),
                Integer.toString(c.waitSeconds()), Integer.toString(c.shrinkSeconds()), Double.toString(c.damageMultiplier())};
        for (int i = 0; i < values.length; i++) {
            var field = new EditBox(font, panel + 8 + (i % 2) * 103, 134 + (i / 2) * 32 - panelScroll, 97, 18,
                    Component.translatable(fieldKey(i)));
            field.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable(fieldKey(i) + ".desc")));
            field.setMaxLength(32); field.setValue(values[i]);
            fields.add(addRenderableWidget(field));
            field.setResponder(value -> { if (fields.size() == 6 && !PoisonWorldEditor.saving) apply(); });
        }
        button(panel + 8, 222, 97, "editor.blockzone.move_up", () -> change(() -> draft.moveCircle(-1)));
        button(panel + 111, 222, 97, "editor.blockzone.move_down", () -> change(() -> draft.moveCircle(1)));
        button(panel + 8, 246, 97, "editor.blockzone.observe", () -> { if (apply()) minecraft.setScreen(null); });
        button(panel + 111, 246, 97, "editor.blockzone.save", () -> { if (apply()) PoisonWorldEditor.save(); });
        button(panel + 8, 270, 200, "setting.battlezone.poisonShape." + draft.path().shape().id(),
                () -> change(() -> draft.setShape(dev.stya.blockzone.util.battlezone.ZoneShape.values()[
                        (draft.path().shape().ordinal() + 1) % dev.stya.blockzone.util.battlezone.ZoneShape.values().length])));
        button(panel + 170, 3, 38, "editor.blockzone.exit", this::onClose);
    }
    private void button(int x, int y, int w, String key, Runnable action) {
        addRenderableWidget(Button.builder(Component.translatable(key), b -> { if (!PoisonWorldEditor.saving) action.run(); })
                .bounds(x, y - panelScroll, w, 20).build());
    }
    private void change(Runnable action) {
        if (!apply()) return;
        try { action.run(); rebuildWidgets(); }
        catch (IllegalArgumentException invalid) { PoisonWorldEditor.message = invalid.getMessage(); }
    }
    private boolean apply() {
        if (fields.size() != 6 || PoisonWorldEditor.saving) return false;
        try {
            double x = number(0), z = number(1), radius = number(2), damage = number(5);
            int wait = Integer.parseInt(fields.get(3).getValue()), transition = Integer.parseInt(fields.get(4).getValue());
            if (radius < 0 || radius > 30000000 || wait < 0 || transition < 0 || wait > 1000000
                    || transition > 1000000 || damage < 0 || damage > Float.MAX_VALUE) throw new IllegalArgumentException();
            PoisonWorldEditor.draft.replace(new PoisonPath.Circle(x, z, radius, wait, transition, damage));
            PoisonWorldEditor.message = ""; return true;
        } catch (RuntimeException invalid) {
            PoisonWorldEditor.message = Component.translatable("editor.blockzone.invalid").getString(); return false;
        }
    }
    private double number(int index) {
        double value = Double.parseDouble(fields.get(index).getValue());
        if (!Double.isFinite(value)) throw new IllegalArgumentException(); return value;
    }
    @Override public void onClose() {
        if (PoisonWorldEditor.saving) return;
        boolean unapplied = false;
        var c = PoisonWorldEditor.draft == null ? null : PoisonWorldEditor.draft.selected();
        if (c != null && fields.size() == 6) {
            String[] values = {Double.toString(c.x()), Double.toString(c.z()), Double.toString(c.radius()),
                    Integer.toString(c.waitSeconds()), Integer.toString(c.shrinkSeconds()), Double.toString(c.damageMultiplier())};
            for (int i = 0; i < 6; i++) if (!values[i].equals(fields.get(i).getValue())) unapplied = true;
        }
        if (PoisonWorldEditor.dirty() || unapplied) {
            minecraft.setScreen(new ConfirmScreen(yes -> { if (yes) PoisonWorldEditor.discard(); else minecraft.setScreen(this); },
                    Component.translatable("editor.blockzone.discard"), Component.translatable("editor.blockzone.discard_hint")));
        } else PoisonWorldEditor.discard();
    }
    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (PoisonWorldEditor.draft == null) return;
        g.fill(panel, 0, width, height, 0xd0182028);
        g.drawString(font, title, panel + 8, 8 - panelScroll, 0xffffff);
        var d = PoisonWorldEditor.draft;
        g.drawCenteredString(font, Component.translatable("editor.blockzone.sequence", d.sequence()+1, d.paths().size()), panel + 108, 33 - panelScroll, 0xffffff);
        g.drawCenteredString(font, Component.translatable("editor.blockzone.circle", d.circle()+1, d.path().circles().size()), panel + 108, 83 - panelScroll, 0xffffff);
        for (int i = 0; i < 6; i++) g.drawString(font, Component.translatable(fieldKey(i)), panel + 8 + (i % 2) * 103, 123+(i / 2)*32 - panelScroll, 0xffffff);
        g.fill(4, 4, Math.max(4, panel - 4), 65, 0xb0000000);
        g.drawString(font, Component.translatable("editor.blockzone.drag_controls"), 8, 8, 0xffffff);
        g.drawString(font, Component.translatable("editor.blockzone.colors"), 8, 24, 0xffffff);
        g.drawString(font, Component.translatable("editor.blockzone.camera_controls"), 8, 40, 0xffffff);
        var c = d.selected();
        String warning = PoisonWorldEditor.message;
        try {
            var actual = new ZoneGeometry(c.x(), PoisonWorldEditor.y(), c.z(), c.radius(), PoisonWorldEditor.draft.path().shape()).fitInside(PoisonWorldEditor.bounds());
            if (warning.isEmpty() && (actual.centerX()!=c.x() || actual.centerZ()!=c.z()))
                warning = Component.translatable("editor.blockzone.adjusted", String.format(java.util.Locale.ROOT, "%.2f", actual.centerX()),
                        String.format(java.util.Locale.ROOT, "%.2f", actual.centerZ())).getString();
        } catch (IllegalArgumentException bad) { warning = Component.translatable("editor.blockzone.too_large").getString(); }
        if (!warning.isEmpty()) g.drawWordWrap(font, Component.literal(warning), 8, 72, Math.max(80, panel - 16), 0xffc060);
        super.render(g, mouseX, mouseY, partialTick);
    }
    private Vec3 ground(double mouseX, double mouseY) {
        var camera = minecraft.gameRenderer.getMainCamera();
        return dev.stya.blockzone.util.editor.PoisonEditorGeometry.ground(camera.getPosition(), camera.rotation(),
                minecraft.options.fov().get(), (double) width / height, 2*mouseX/width-1, 1-2*mouseY/height,
                PoisonWorldEditor.y());
    }
    private void manipulate(double x, double y) {
        var point = ground(x,y);
        if (point == null) { PoisonWorldEditor.message = Component.translatable("editor.blockzone.look_down").getString(); return; }
        var c = PoisonWorldEditor.draft.selected();
        if (resizing) PoisonWorldEditor.setGeometry(c.x(), c.z(), (PoisonWorldEditor.draft.path().shape() == dev.stya.blockzone.util.battlezone.ZoneShape.SQUARE_PRISM
                ? Math.max(Math.abs(point.x-c.x()), Math.abs(point.z-c.z())) : Math.hypot(point.x-c.x(), point.z-c.z())));
        else PoisonWorldEditor.setGeometry(point.x, point.z, c.radius());
        var next = PoisonWorldEditor.draft.selected();
        fields.get(0).setValue(Double.toString(next.x())); fields.get(1).setValue(Double.toString(next.z()));
        fields.get(2).setValue(Double.toString(next.radius()));
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
        setFocused(null);
        if (PoisonWorldEditor.saving) return true;
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && apply()) {
            moving = true; resizing = hasShiftDown(); manipulate(x,y); return true;
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
            fields.get(2).setValue(Double.toString(PoisonWorldEditor.draft.selected().radius())); return true;
        }
        if (x >= panel && !PoisonWorldEditor.saving && apply()) {
            panelScroll = net.minecraft.util.Mth.clamp(panelScroll - (int)(delta * 18), 0, Math.max(0, 294 - height));
            rebuildWidgets(); return true;
        }
        return super.mouseScrolled(x,y,delta);
    }
}
