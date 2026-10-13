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
    private static int keysWidth(Font font, Prompt prompt) {
        return prompt.keys.stream().mapToInt(key -> font.width(label(key)) + 12).sum() + (prompt.keys.size() - 1) * 3;
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
            float scale = Math.min(1.5F, 1040F / total);
            gui.pose().pushPose(); gui.pose().translate((1040 - total * scale) / 2, 382 + row * 24, 0); gui.pose().scale(scale, scale, 1);
            int x = 0;
            for (var prompt : rowPrompts) {
                int width = keysWidth(font, prompt);
                int keyX = x;
                for (var key : prompt.keys) {
                    String keyLabel = label(key);
                    int keyWidth = font.width(keyLabel) + 12;
                    boolean pressed = key.equals(options.keyShift.getKey()) && shift;
                    boolean drawn = false;
                    if (smartRenderer != null) {
                        try { smartRenderer.invoke(null, gui, keyX, 0, keyLabel, pressed); drawn = true; }
                        catch (ReflectiveOperationException exception) { smartRenderer = null; }
                    }
                    if (!drawn) {
                        gui.fill(keyX, 0, keyX + keyWidth, 13, pressed ? 0xCC9BABB5 : 0xAA25313A);
                        gui.drawCenteredString(font, keyLabel, keyX + keyWidth / 2, 3, 0xFFF3F7F8);
                    }
                    keyX += keyWidth + 3;
                }
                var description = Component.translatable(prompt.description);
                gui.drawString(font, description, x + width + 6, 3, 0xFFF3F7F8, false);
                x += width + font.width(description) + 22;
            }
            gui.pose().popPose();
        }
    }

    private InventoryPromptRenderer() {}
}
