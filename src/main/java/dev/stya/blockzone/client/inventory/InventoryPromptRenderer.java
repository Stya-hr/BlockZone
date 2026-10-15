package dev.stya.blockzone.client.inventory;

import java.lang.reflect.Method;
import java.util.List;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.fml.ModList;

/** Uses Smart Key Prompts' public key renderer when installed; no hard dependency. */
public final class InventoryPromptRenderer {
    private static boolean resolved;
    private static Method smartRenderer;
    private static Method smartTranslation;
    private static final int KEY_HEIGHT = 16;
    private static final float LABEL_SCALE = 1.12f;
    private record Prompt(List<InputConstants.Key> keys, String description) {}
    private static void resolve() {
        if (resolved) return;
        resolved = true;
        if (ModList.get().isLoaded("smartkeyprompts")) {
            try {
                smartRenderer = Class.forName("com.mafuyu404.smartkeyprompts.init.KeyRenderer")
                        .getMethod("drawKeyBoardKey", GuiGraphics.class, int.class, int.class, String.class, boolean.class);
                smartTranslation = Class.forName("com.mafuyu404.smartkeyprompts.util.KeyUtils")
                        .getMethod("translateKey", String.class);
            } catch (ReflectiveOperationException exception) {
                smartRenderer = null;
                smartTranslation = null;
                org.apache.logging.log4j.LogManager.getLogger().warn("Smart Key Prompts renderer unavailable; using inventory keycaps", exception);
            }
        }
    }
    public static boolean usesSmartKeyPrompts() { resolve(); return smartRenderer != null; }
    private static String label(InputConstants.Key key) {
        if (smartTranslation != null) {
            try { return (String) smartTranslation.invoke(null, key.getName()); }
            catch (ReflectiveOperationException exception) { smartTranslation = null; }
        }
        return key.getDisplayName().getString();
    }
    private static int keyWidth(Font font, InputConstants.Key key) {
        return font.width(label(key)) + 12;
    }
    public static int keyWidth(Font font, InputConstants.Key key, int height) {
        resolve();
        if (smartRenderer != null) return (int)Math.ceil(keyWidth(font, key) * height / (float)KEY_HEIGHT);
        float scale = height / (float)KEY_HEIGHT * 1.1f;
        return Math.max(height, (int)Math.ceil(font.width(key.getDisplayName()) * scale) + 4);
    }
    private static int keysWidth(Font font, Prompt prompt) {
        return prompt.keys.stream().mapToInt(key -> keyWidth(font, key, KEY_HEIGHT)).sum() + (prompt.keys.size() - 1) * 3;
    }
    public static void render(GuiGraphics gui, Font font, boolean shift) {
        resolve();
        var options = Minecraft.getInstance().options;
        var left = options.keyAttack.getKey();
        var right = options.keyUse.getKey();
        var prompts = List.of(
                new Prompt(List.of(left), "inventory.blockzone.drop_one"),
                new Prompt(List.of(options.keyShift.getKey(), left), "inventory.blockzone.drop_ten"),
                new Prompt(List.of(right), "inventory.blockzone.drop_all"),
                new Prompt(List.of(left), "inventory.blockzone.drag_swap"),
                new Prompt(List.of(options.keyInventory.getKey()), "inventory.blockzone.close_action"));
        var rows = List.of(prompts.subList(0, 3), prompts.subList(3, prompts.size()));
        int[] widths = rows.stream().mapToInt(row -> row.stream().mapToInt(prompt ->
                keysWidth(font, prompt) + (int)Math.ceil(font.width(Component.translatable(prompt.description)) * LABEL_SCALE) + 17).sum() - 12).toArray();
        float scale = Math.min(2F, 1016F / Math.max(widths[0], widths[1]));
        for (int row = 0; row < rows.size(); row++) {
            gui.pose().pushPose();
            try {
                gui.pose().translate((1040 - widths[row] * scale) / 2, 380 + row * (KEY_HEIGHT + 8) * scale, 0);
                gui.pose().scale(scale, scale, 1);
                int x = 0;
                for (var prompt : rows.get(row)) {
                    int width = keysWidth(font, prompt);
                    var description = Component.translatable(prompt.description);
                    int descriptionWidth = (int)Math.ceil(font.width(description) * LABEL_SCALE);
                    int keyX = x;
                    for (var key : prompt.keys) {
                        keyX += renderKey(gui, font, key, keyX, 0, KEY_HEIGHT,
                                key.equals(options.keyShift.getKey()) && shift) + 3;
                    }
                    gui.pose().pushPose();
                    gui.pose().translate(x + width + 5, (KEY_HEIGHT - font.lineHeight * LABEL_SCALE) / 2, 0);
                    gui.pose().scale(LABEL_SCALE, LABEL_SCALE, 1);
                    textWithShadow(gui, font, description, 0xFFF3F7F8);
                    gui.pose().popPose();
                    x += width + 5 + descriptionWidth + 12;
                }
            } finally { gui.pose().popPose(); }
        }
    }

    /** Single-key HUD prompt with a caller-selected height. Uses the registered key, never a glyph alias. */
    public static int renderKey(GuiGraphics gui, Font font, InputConstants.Key key, int x, int y, int height) {
        return renderKey(gui, font, key, x, y, height, false);
    }
    private static int renderKey(GuiGraphics gui, Font font, InputConstants.Key key, int x, int y, int height, boolean held) {
        resolve();
        boolean pressed = held || key.getType() == InputConstants.Type.KEYSYM && key.getValue() >= 0
                && InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), key.getValue());
        if (key.getType() == InputConstants.Type.MOUSE)
            pressed |= org.lwjgl.glfw.GLFW.glfwGetMouseButton(Minecraft.getInstance().getWindow().getWindow(), key.getValue())
                    == org.lwjgl.glfw.GLFW.GLFW_PRESS;
        gui.pose().pushPose();
        try {
            gui.pose().translate(x, y, 0);
            int measuredWidth = keyWidth(font, key, height);
            if (smartRenderer != null) {
                gui.flush();
                float scale = height / (float)KEY_HEIGHT;
                gui.pose().scale(scale, scale, 1);
                int width = keyWidth(font, key);
                drawKey(gui, font, key, 0, width, pressed);
                return measuredWidth;
            }
            String text = key.getDisplayName().getString();
            float textScale = height / (float)KEY_HEIGHT * 1.1f;
            int width = keyWidth(font, key, height);
            gui.fill(0, 0, width, height, pressed ? 0xFFE0E0E0 : 0xFFF3F7F8);
            gui.pose().translate((width - font.width(text) * textScale) / 2,
                    (height - font.lineHeight * textScale) / 2, 0);
            gui.pose().scale(textScale, textScale, 1);
            gui.drawString(font, text, 0, 0, 0xFF10181E, false);
            return width;
        } finally { gui.pose().popPose(); }
    }

    /** Feather the glyph mask itself; never draw a rectangular prompt backdrop. */
    public static void textWithShadow(GuiGraphics gui, Font font, Component text, int color) {
        for (int ring = 1; ring <= 2; ring++) {
            float radius = ring * .65f;
            int shadow = ring == 1 ? 0x16000000 : 0x08000000;
            for (int dx = -1; dx <= 1; dx++) for (int dy = -1; dy <= 1; dy++) {
                if (dx == 0 && dy == 0) continue;
                gui.pose().pushPose();
                gui.pose().translate(dx * radius, dy * radius + .3f, 0);
                gui.drawString(font, text, 0, 0, shadow, false);
                gui.pose().popPose();
            }
        }
        gui.drawString(font, text, 0, 0, color, false);
    }

    private static void drawKey(GuiGraphics gui, Font font, InputConstants.Key key, int x, int width, boolean pressed) {
        if (smartRenderer != null) {
            try { smartRenderer.invoke(null, gui, x, 0, label(key), pressed); return; }
            catch (ReflectiveOperationException exception) { smartRenderer = null; smartTranslation = null; }
        }
        gui.fill(x, 0, x + width, KEY_HEIGHT, pressed ? 0xFFE0E0E0 : 0xFFFFFFFF);
        String text = key.getDisplayName().getString();
        gui.drawString(font, text, x + (width - font.width(text)) / 2, 3, 0xFF182028, false);
    }

    private InventoryPromptRenderer() {}
}
