package dev.stya.blockzone.client.editor;

import dev.stya.blockzone.editor.zone.PoisonEditorGeometry;
import dev.stya.blockzone.zone.PoisonPath;
import dev.stya.blockzone.zone.ZoneGeometry;
import dev.stya.blockzone.zone.ZoneShape;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

/** A fixed action bar and a scrollable inspector beside the live world preview. */
public final class PoisonEditorScreen extends Screen {
    private static final String[] FIELD_KEYS = {"x", "z", "radius", "waitSeconds", "shrinkSeconds", "damageMultiplier"};
    private static final int MARGIN = 8;
    private static final int CONTENT_HEIGHT = 348;
    private final List<EditBox> fields = new ArrayList<>();
    private final Map<AbstractWidget, Integer> content = new LinkedHashMap<>();
    private final List<AbstractWidget> fixed = new ArrayList<>();
    private int panel, panelWidth, bodyTop, bodyBottom, footerTop, panelScroll;
    private boolean moving, looking, updatingFields, invalidInput, dirty;
    private Button save, removeSequence, removeCircle, copySequence, copyCircle, moveUp, moveDown;

    public PoisonEditorScreen() {
        super(Component.translatable("editor.blockzone.title"));
    }

    private static Component ui(String key) {
        return Component.translatable("editor.blockzone.ui." + key);
    }

    private static String fieldKey(int index) {
        return "setting.battlezone.poisonCircle." + FIELD_KEYS[index];
    }

    static double viewportCenterX(int guiWidth) {
        return (guiWidth - MARGIN - EditorUi.sidebarWidth(guiWidth)) / 2.0;
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    protected void init() {
        if (PoisonWorldEditor.draft == null) { onClose(); return; }
        // Screen.init also runs on resize and when returning from discard confirmation.
        String[] values = fields.size() == 6 ? fields.stream().map(EditBox::getValue).toArray(String[]::new) : values();
        content.clear();
        fixed.clear();
        fields.clear();
        moving = looking = false;
        setDragging(false);
        panelWidth = EditorUi.sidebarWidth(width);
        panel = width - MARGIN - panelWidth;
        bodyTop = 62;
        footerTop = height - MARGIN - 74;
        bodyBottom = footerTop - 6;
        panelScroll = Mth.clamp(panelScroll, 0, maxScroll());
        int x = panel + 12, inner = panelWidth - 24, half = (inner - 8) / 2;
        var draft = PoisonWorldEditor.draft;

        fixed.add(addRenderableWidget(EditorUi.button(panel + panelWidth - 48, 27, 36, 22,
                Component.translatable("editor.blockzone.exit"), EditorUi.Tone.DEFAULT, this::onClose)));
        contentButton(x, 16, 24, Component.literal("<"), ui("previous_sequence"), EditorUi.Tone.DEFAULT,
                () -> change(() -> draft.selectSequence(draft.sequence() - 1)));
        contentButton(x + inner - 24, 16, 24, Component.literal(">"), ui("next_sequence"), EditorUi.Tone.DEFAULT,
                () -> change(() -> draft.selectSequence(draft.sequence() + 1)));
        copySequence = contentButton(x, 44, half, "copy_sequence", EditorUi.Tone.DEFAULT, () -> change(draft::duplicateSequence));
        removeSequence = contentButton(x + half + 8, 44, half, "delete_sequence", EditorUi.Tone.DANGER, () -> change(draft::removeSequence));
        contentButton(x, 100, 24, Component.literal("<"), ui("previous_circle"), EditorUi.Tone.DEFAULT,
                () -> change(() -> draft.selectCircle(draft.circle() - 1)));
        contentButton(x + inner - 24, 100, 24, Component.literal(">"), ui("next_circle"), EditorUi.Tone.DEFAULT,
                () -> change(() -> draft.selectCircle(draft.circle() + 1)));
        copyCircle = contentButton(x, 128, half, "copy_circle", EditorUi.Tone.DEFAULT, () -> change(draft::addCircle));
        removeCircle = contentButton(x + half + 8, 128, half, "delete_circle", EditorUi.Tone.DANGER, () -> change(draft::removeCircle));
        moveUp = contentButton(x, 156, half, "move_up", EditorUi.Tone.DEFAULT, () -> change(() -> draft.moveCircle(-1)));
        moveDown = contentButton(x + half + 8, 156, half, "move_down", EditorUi.Tone.DEFAULT, () -> change(() -> draft.moveCircle(1)));

        updatingFields = true;
        for (int i = 0; i < values.length; i++) {
            int offset = 214 + (i / 2) * 36;
            var field = EditorUi.input(font, x + (i % 2) * (half + 8), bodyTop + offset - panelScroll, half, 20,
                    Component.translatable(fieldKey(i)));
            field.setTooltip(Tooltip.create(Component.translatable(fieldKey(i) + ".desc")));
            field.setMaxLength(32);
            field.setValue(values[i]);
            fields.add(addContent(field, offset));
            field.setResponder(value -> { if (!updatingFields && !PoisonWorldEditor.saving) apply(); });
        }
        updatingFields = false;
        Component shape = Component.translatable("setting.battlezone.poisonShape." + draft.path().shape().id());
        contentButton(x, 320, inner, shape, shape.copy().append("\n").append(ui("shape_hint")),
                EditorUi.Tone.DEFAULT, () -> change(() -> draft.setShape(ZoneShape.values()[
                        (draft.path().shape().ordinal() + 1) % ZoneShape.values().length])));

        fixed.add(addRenderableWidget(EditorUi.button(x, footerTop + 40, half, 24, ui("observe"), EditorUi.Tone.DEFAULT,
                () -> { if (apply()) minecraft.setScreen(null); })));
        fixed.get(fixed.size() - 1).setTooltip(Tooltip.create(Component.translatable("editor.blockzone.observe")));
        save = addRenderableWidget(EditorUi.button(x + half + 8, footerTop + 40, half, 24, ui("save"), EditorUi.Tone.PRIMARY,
                () -> { if (apply()) PoisonWorldEditor.save(); }));
        save.setTooltip(Tooltip.create(Component.translatable("editor.blockzone.save")));
        fixed.add(save);
        validateFields();
        updateActions();
    }

    private String[] values() {
        var circle = PoisonWorldEditor.draft.selected();
        return new String[] {Double.toString(circle.x()), Double.toString(circle.z()), Double.toString(circle.radius()),
                Integer.toString(circle.waitSeconds()), Integer.toString(circle.shrinkSeconds()), Double.toString(circle.damageMultiplier())};
    }

    private <T extends AbstractWidget> T addContent(T widget, int offset) {
        content.put(widget, offset);
        return addWidget(widget);
    }

    private Button contentButton(int x, int y, int width, String key, EditorUi.Tone tone, Runnable action) {
        Component label = Component.translatable("editor.blockzone." + key);
        return contentButton(x, y, width, label, label, tone, action);
    }

    private Button contentButton(int x, int y, int width, Component label, Component hint, EditorUi.Tone tone, Runnable action) {
        Button button = EditorUi.button(x, bodyTop + y - panelScroll, width, 22, label, tone,
                () -> { if (!PoisonWorldEditor.saving) action.run(); });
        button.setTooltip(Tooltip.create(hint));
        return addContent(button, y);
    }

    private void change(Runnable action) {
        if (!apply()) return;
        try {
            action.run();
            fields.clear();
            rebuildWidgets();
        } catch (IllegalArgumentException invalid) {
            PoisonWorldEditor.message = invalid.getMessage();
        }
    }

    private boolean validateFields() {
        if (fields.size() != 6) return false;
        boolean valid = true;
        for (int i = 0; i < fields.size(); i++) {
            boolean fieldValid = true;
            try {
                double value = number(i);
                if (i == 3 || i == 4) {
                    int seconds = Integer.parseInt(fields.get(i).getValue());
                    if (seconds < 0 || seconds > 1000000) fieldValid = false;
                } else if (i == 2) fieldValid = value >= 0 && value <= 30000000;
                else if (i == 5) fieldValid = value >= 0 && value <= Float.MAX_VALUE;
            } catch (IllegalArgumentException invalid) {
                fieldValid = false;
            }
            EditorUi.invalid(fields.get(i), !fieldValid);
            valid &= fieldValid;
        }
        invalidInput = !valid;
        return valid;
    }

    private boolean apply() {
        if (PoisonWorldEditor.saving || !validateFields()) {
            if (!PoisonWorldEditor.saving) PoisonWorldEditor.message = Component.translatable("editor.blockzone.invalid").getString();
            return false;
        }
        PoisonWorldEditor.draft.replace(new PoisonPath.Circle(number(0), number(1), number(2),
                Integer.parseInt(fields.get(3).getValue()), Integer.parseInt(fields.get(4).getValue()), number(5)));
        PoisonWorldEditor.message = "";
        updateActions();
        return true;
    }

    private double number(int index) {
        double value = Double.parseDouble(fields.get(index).getValue());
        if (!Double.isFinite(value)) throw new IllegalArgumentException();
        return value;
    }

    private void updateActions() {
        var draft = PoisonWorldEditor.draft;
        if (draft == null || save == null) return;
        dirty = PoisonWorldEditor.dirty();
        boolean active = !PoisonWorldEditor.saving;
        for (var widget : content.keySet()) widget.active = active;
        for (var widget : fixed) widget.active = active;
        fields.forEach(field -> field.setEditable(active));
        save.active = active && dirty && !invalidInput;
        save.setMessage(PoisonWorldEditor.saving ? ui("saving_button") : ui("save"));
        removeSequence.active = active && draft.paths().size() > 1;
        copySequence.active = active && draft.paths().size() < 128;
        removeCircle.active = active && draft.path().circles().size() > 1;
        copyCircle.active = active && draft.path().circles().size() < 128;
        moveUp.active = active && draft.circle() > 0;
        moveDown.active = active && draft.circle() < draft.path().circles().size() - 1;
    }

    @Override
    public void tick() {
        fields.forEach(EditBox::tick);
        updateActions();
    }

    @Override
    public void onClose() {
        if (PoisonWorldEditor.saving) return;
        if (PoisonWorldEditor.dirty() || invalidInput) {
            minecraft.setScreen(new ConfirmScreen(yes -> {
                if (yes) PoisonWorldEditor.discard(); else minecraft.setScreen(this);
            }, Component.translatable("editor.blockzone.discard"), Component.translatable("editor.blockzone.discard_hint")));
        } else PoisonWorldEditor.discard();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (PoisonWorldEditor.draft == null) return;
        EditorUi.panel(graphics, panel, MARGIN, panelWidth, height - MARGIN * 2);
        graphics.fill(panel + 12, 20, panel + 15, 47, EditorUi.ACCENT);
        EditorUi.text(graphics, font, Component.literal("BLOCKZONE"), panel + 22, 18, panelWidth - 80, EditorUi.MUTED);
        EditorUi.text(graphics, font, title, panel + 22, 34, panelWidth - 78, EditorUi.TEXT);
        graphics.fill(panel + 12, 55, panel + panelWidth - 12, 56, EditorUi.BORDER);

        int x = panel + 12, inner = panelWidth - 24, half = (inner - 8) / 2;
        graphics.enableScissor(panel + 1, bodyTop, panel + panelWidth - 1, bodyBottom);
        var draft = PoisonWorldEditor.draft;
        EditorUi.section(graphics, font, ui("sequence_section"), x, contentY(0), inner);
        EditorUi.card(graphics, x + 28, contentY(16), inner - 56, 22);
        centered(graphics, Component.translatable("editor.blockzone.sequence", draft.sequence() + 1, draft.paths().size()), contentY(23), inner - 64);
        EditorUi.section(graphics, font, ui("circle_section"), x, contentY(84), inner);
        EditorUi.card(graphics, x + 28, contentY(100), inner - 56, 22);
        centered(graphics, Component.translatable("editor.blockzone.circle", draft.circle() + 1, draft.path().circles().size()), contentY(107), inner - 64);
        EditorUi.section(graphics, font, ui("parameters_section"), x, contentY(190), inner);
        for (int i = 0; i < fields.size(); i++) {
            EditorUi.text(graphics, font, Component.translatable(fieldKey(i)), x + (i % 2) * (half + 8),
                    contentY(202 + (i / 2) * 36), half, EditorUi.MUTED);
        }
        boolean overBody = mouseX >= panel && mouseY >= bodyTop && mouseY < bodyBottom;
        for (var widget : content.keySet()) {
            if (widget.getY() + widget.getHeight() >= bodyTop && widget.getY() <= bodyBottom)
                widget.render(graphics, overBody ? mouseX : -1, overBody ? mouseY : -1, partialTick);
        }
        graphics.disableScissor();
        EditorUi.scrollbar(graphics, panel + panelWidth - 5, bodyTop, bodyBottom - bodyTop, CONTENT_HEIGHT, panelScroll);

        graphics.fill(panel + 1, footerTop, panel + panelWidth - 1, height - MARGIN - 1, 0xff16232e);
        graphics.fill(panel + 12, footerTop, panel + panelWidth - 12, footerTop + 1, EditorUi.BORDER);
        Status status = status();
        graphics.fill(x, footerTop + 11, x + 4, footerTop + 15, status.color());
        var lines = font.split(status.text(), inner - 12);
        for (int i = 0; i < Math.min(2, lines.size()); i++)
            graphics.drawString(font, lines.get(i), x + 12, footerTop + 8 + i * 11, status.color(), false);
        renderWorldGuide(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        if (mouseX >= x && mouseX < x + inner && mouseY >= footerTop + 4 && mouseY < footerTop + 35)
            graphics.renderTooltip(font, font.split(status.text(), Math.min(280, width - 24)), mouseX, mouseY);
        else if (mouseX >= MARGIN && mouseX < panel - MARGIN && mouseY >= height - 54 && mouseY < height - MARGIN)
            graphics.renderTooltip(font, font.split(Component.translatable("editor.blockzone.camera_controls")
                    .append("\n").append(Component.translatable("editor.blockzone.drag_controls"))
                    .append("\n").append(Component.translatable("editor.blockzone.colors")), Math.min(280, width - 24)), mouseX, mouseY);
    }

    private void centered(GuiGraphics graphics, Component label, int y, int maxWidth) {
        int textWidth = Math.min(font.width(label), maxWidth);
        EditorUi.text(graphics, font, label, panel + (panelWidth - textWidth) / 2, y, maxWidth, EditorUi.TEXT);
    }

    private void renderWorldGuide(GuiGraphics graphics) {
        int available = panel - MARGIN * 2;
        if (available <= 24) return;
        EditorUi.panel(graphics, MARGIN, MARGIN, available, 36);
        graphics.fill(16, 18, 19, 22, EditorUi.ACCENT);
        EditorUi.text(graphics, font, ui("preview"), 24, 16, available - 26, EditorUi.TEXT);
        EditorUi.text(graphics, font, ui("focus_hint"), 16, 30, available - 16, EditorUi.MUTED);
        EditorUi.panel(graphics, MARGIN, height - 54, available, 46);
        EditorUi.text(graphics, font, ui("drag_hint"), 16, height - 46, available - 16, EditorUi.TEXT);
        EditorUi.text(graphics, font, ui("fly_hint"), 16, height - 34, available - 16, EditorUi.MUTED);
        int legendX = 16;
        int[] colors = {EditorUi.TEXT, EditorUi.WARNING, EditorUi.ACCENT};
        String[] labels = {"legend_selected", "legend_outside", "legend_actual"};
        int cell = (available - 16) / 3;
        for (int i = 0; i < labels.length; i++) {
            graphics.fill(legendX, height - 18, legendX + 4, height - 14, colors[i]);
            EditorUi.text(graphics, font, ui(labels[i]), legendX + 7, height - 20, cell - 9, EditorUi.MUTED);
            legendX += cell;
        }
    }

    private Status status() {
        if (PoisonWorldEditor.saving) return new Status(ui("saving"), EditorUi.ACCENT);
        if (invalidInput) return new Status(Component.translatable("editor.blockzone.invalid"), EditorUi.DANGER);
        String message = PoisonWorldEditor.message;
        if (!message.isEmpty()) return new Status(Component.literal(message), EditorUi.WARNING);
        var circle = PoisonWorldEditor.draft.selected();
        try {
            var actual = new ZoneGeometry(circle.x(), PoisonWorldEditor.y(), circle.z(), circle.radius(),
                    PoisonWorldEditor.draft.path().shape()).fitInside(PoisonWorldEditor.bounds());
            if (actual.centerX() != circle.x() || actual.centerZ() != circle.z())
                return new Status(Component.translatable("editor.blockzone.adjusted", String.format(Locale.ROOT, "%.2f", actual.centerX()),
                        String.format(Locale.ROOT, "%.2f", actual.centerZ())), EditorUi.WARNING);
        } catch (IllegalArgumentException tooLarge) {
            return new Status(Component.translatable("editor.blockzone.too_large"), EditorUi.DANGER);
        }
        return new Status(ui(dirty ? "unsaved" : "saved"), dirty ? EditorUi.WARNING : EditorUi.MUTED);
    }

    private record Status(Component text, int color) {}

    private int contentY(int y) { return bodyTop + y - panelScroll; }
    private int maxScroll() { return Math.max(0, CONTENT_HEIGHT - Math.max(1, bodyBottom - bodyTop)); }

    private void scrollTo(int offset) {
        panelScroll = Mth.clamp(offset, 0, maxScroll());
        content.forEach((widget, y) -> widget.setY(contentY(y)));
    }

    @Override
    public void setFocused(GuiEventListener focused) {
        super.setFocused(focused);
        if (focused instanceof AbstractWidget widget && content.containsKey(widget)) {
            int labelHeight = widget instanceof EditBox ? 14 : 2;
            if (widget.getY() - labelHeight < bodyTop) scrollTo(content.get(widget) - labelHeight);
            else if (widget.getY() + widget.getHeight() > bodyBottom)
                scrollTo(content.get(widget) + widget.getHeight() - (bodyBottom - bodyTop) + 2);
        }
    }

    private Vec3 ground(double mouseX, double mouseY) {
        var camera = minecraft.gameRenderer.getMainCamera();
        return PoisonEditorGeometry.ground(camera.getPosition(), camera.rotation(), minecraft.options.fov().get(),
                (double) width / height, 2 * mouseX / width - 1, 1 - 2 * mouseY / height, PoisonWorldEditor.y());
    }

    private void manipulate(double x, double y) {
        var point = ground(x, y);
        if (point == null) {
            PoisonWorldEditor.message = Component.translatable("editor.blockzone.look_down").getString();
            return;
        }
        var circle = PoisonWorldEditor.draft.selected();
        PoisonWorldEditor.setGeometry(point.x, point.z, circle.radius());
        PoisonWorldEditor.message = "";
        syncGeometry();
    }

    private void syncGeometry() {
        updatingFields = true;
        String[] values = values();
        for (int i = 0; i < 3; i++) fields.get(i).setValue(values[i]);
        updatingFields = false;
        validateFields();
        updateActions();
    }

    private boolean inWorld(double x, double y) {
        return x >= MARGIN && x < panel - MARGIN && y >= 48 && y < height - 58;
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        if (key == GLFW.GLFW_KEY_F6) { PoisonEditorCamera.focus(); return true; }
        if (PoisonEditorCamera.key(key, scan, GLFW.GLFW_PRESS)) return true;
        return super.keyPressed(key, scan, modifiers);
    }

    @Override
    public boolean keyReleased(int key, int scan, int modifiers) {
        PoisonEditorCamera.key(key, scan, GLFW.GLFW_RELEASE);
        return super.keyReleased(key, scan, modifiers);
    }

    @Override
    public void removed() {
        moving = looking = false;
        PoisonEditorCamera.clearMovement();
        super.removed();
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        if (PoisonWorldEditor.saving) return true;
        if (x >= panel) {
            PoisonEditorCamera.clearMovement();
            // Hit testing uses the same viewport as rendering, including the fixed footer.
            var widgets = y >= bodyTop && y < bodyBottom ? content.keySet() : fixed;
            for (var widget : List.copyOf(widgets)) {
                if (widget.mouseClicked(x, y, button)) {
                    if (minecraft.screen == this && children().contains(widget)) {
                        setFocused(widget);
                        if (button == 0) setDragging(true);
                    }
                    return true;
                }
            }
            setFocused(null);
            return true;
        }
        setFocused(null);
        if (!inWorld(x, y)) return true;
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && apply()) {
            moving = true;
            manipulate(x, y);
            return true;
        }
        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) { looking = true; return true; }
        return super.mouseClicked(x, y, button);
    }

    @Override
    public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
        if (inWorld(x, y) && !PoisonWorldEditor.saving) {
            if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT && looking) { PoisonEditorCamera.turn(dx, dy); return true; }
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && moving) { manipulate(x, y); return true; }
        }
        return super.mouseDragged(x, y, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double x, double y, int button) {
        moving = looking = false;
        return super.mouseReleased(x, y, button);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double delta) {
        if (PoisonWorldEditor.saving) return true;
        if (x >= panel && y >= bodyTop && y < bodyBottom) {
            scrollTo(panelScroll - (int) (delta * 24));
            if (getFocused() instanceof AbstractWidget widget && content.containsKey(widget)
                    && (widget.getY() < bodyTop || widget.getY() + widget.getHeight() > bodyBottom)) setFocused(null);
            return true;
        }
        if (inWorld(x, y) && apply()) {
            var circle = PoisonWorldEditor.draft.selected();
            PoisonWorldEditor.setGeometry(circle.x(), circle.z(), circle.radius() + delta * (hasControlDown() ? 10 : 1));
            syncGeometry();
            return true;
        }
        return super.mouseScrolled(x, y, delta);
    }
}
