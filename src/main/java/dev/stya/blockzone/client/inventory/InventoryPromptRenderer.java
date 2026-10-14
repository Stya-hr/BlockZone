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
    private static final int ICON_KEY_WIDTH = 24;
    private static final int KEY_HEIGHT = 16;
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
        return InventoryKeyIcons.icon(key.getName()) == null ? font.width(label(key)) + 12 : ICON_KEY_WIDTH;
    }
    private static int keysWidth(Font font, Prompt prompt) {
        return prompt.keys.stream().mapToInt(key -> keyWidth(font, key)).sum() + (prompt.keys.size() - 1) * 3;
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
                new Prompt(List.of(Minecraft.getInstance().options.keyInventory.getKey()), "inventory.blockzone.close_action"));
        var rows = List.of(prompts.subList(0, 3), prompts.subList(3, prompts.size()));
        for (int row = 0; row < rows.size(); row++) {
            var rowPrompts = rows.get(row);
            int total = rowPrompts.stream().mapToInt(p -> keysWidth(font, p) + font.width(Component.translatable(p.description)) + 22).sum();
            float scale = Math.min(2F, 1040F / total);
            gui.pose().pushPose(); gui.pose().translate((1040 - total * scale) / 2, 382 + row * 32, 0); gui.pose().scale(scale, scale, 1);
            int x = 0;
            for (var prompt : rowPrompts) {
                int width = keysWidth(font, prompt);
                int keyX = x;
                for (var key : prompt.keys) {
                    int keyWidth = keyWidth(font, key);
                    boolean pressed = key.getType() == InputConstants.Type.KEYSYM && key.getValue() >= 0
                            && InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), key.getValue());
                    if (key.getType() == InputConstants.Type.MOUSE)
                        pressed = org.lwjgl.glfw.GLFW.glfwGetMouseButton(Minecraft.getInstance().getWindow().getWindow(), key.getValue())
                                == org.lwjgl.glfw.GLFW.GLFW_PRESS;
                    if (key.equals(options.keyShift.getKey())) pressed |= shift;
                    drawKey(gui, font, key, keyX, keyWidth, pressed);
                    keyX += keyWidth + 3;
                }
                var description = Component.translatable(prompt.description);
                gui.drawString(font, description, x + width + 6, 3, 0xFFF3F7F8, false);
                x += width + font.width(description) + 22;
            }
            gui.pose().popPose();
        }
    }

    private static void drawKey(GuiGraphics gui, Font font, InputConstants.Key key, int x, int width, boolean pressed) {
        String icon = InventoryKeyIcons.icon(key.getName());
        if (icon == null && smartRenderer != null) {
            try { smartRenderer.invoke(null, gui, x, 0, label(key), pressed); return; }
            catch (ReflectiveOperationException exception) { smartRenderer = null; }
        }
        if (smartRenderer != null) {
            var texture = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("smartkeyprompts",
                    "textures/gui/" + (pressed ? "pressed_key" : "unpressed_key") + ".png");
            gui.blit(texture, x, 0, 6, KEY_HEIGHT, 0, 0, 6, 16, 32, 16);
            gui.blit(texture, x + 6, 0, width - 12, KEY_HEIGHT, 6, 0, 20, 16, 32, 16);
            gui.blit(texture, x + width - 6, 0, 6, KEY_HEIGHT, 26, 0, 6, 16, 32, 16);
        } else {
            gui.fill(x, 0, x + width, KEY_HEIGHT, pressed ? 0xCC9BABB5 : 0xAA25313A);
        }
        if (icon != null) {
            var texture = InventorySvgAssets.get("key_" + icon, 48, 48);
            gui.blit(texture.id(), x + (width - 12) / 2, pressed ? 3 : 1, 12, 12,
                    0, 0, texture.width(), texture.height(), texture.width(), texture.height());
        } else {
            gui.drawCenteredString(font, label(key), x + width / 2, 3, 0xFFF3F7F8);
        }
    }

    private InventoryPromptRenderer() {}
}
