package dev.stya.blockzone.client.inventory;

import java.lang.reflect.Method;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.fml.ModList;

/** Uses Smart Key Prompts' public key renderer when installed; no hard dependency. */
public final class InventoryPromptRenderer {
    private static boolean resolved;
    private static Method smartRenderer;
    private record Prompt(String key, String description, boolean pressed) {}
    private static void resolve() {
        if (resolved) return;
        resolved = true;
        if (ModList.get().isLoaded("smartkeyprompts")) {
            try {
                smartRenderer = Class.forName("com.mafuyu404.smartkeyprompts.init.KeyRenderer")
                        .getMethod("drawKeyBoardKey", GuiGraphics.class, int.class, int.class, String.class, boolean.class);
            } catch (ReflectiveOperationException exception) {
                org.apache.logging.log4j.LogManager.getLogger().warn("Smart Key Prompts renderer unavailable; using inventory keycaps", exception);
            }
        }
    }
    public static boolean usesSmartKeyPrompts() { resolve(); return smartRenderer != null; }
    public static void render(GuiGraphics gui, Font font, boolean shift) {
        resolve();
        var prompts = List.of(
                new Prompt(Component.translatable("key.mouse.left").getString(), "inventory.blockzone.drop_one", false),
                new Prompt(Component.translatable("key.keyboard.left.shift").getString() + "+" + Component.translatable("key.mouse.left").getString(), "inventory.blockzone.drop_ten", shift),
                new Prompt(Component.translatable("key.mouse.right").getString(), "inventory.blockzone.drop_all", false),
                new Prompt(InventoryPreviewKeys.OPEN.getTranslatedKeyMessage().getString(), "inventory.blockzone.close_action", false));
        int total = prompts.stream().mapToInt(p -> font.width(p.key) + font.width(Component.translatable(p.description)) + 34).sum();
        float scale = Math.min(1.5F, 1040F / total);
        gui.pose().pushPose(); gui.pose().translate((1040 - total * scale) / 2, 382, 0); gui.pose().scale(scale, scale, 1);
        int x = 0;
        for (var prompt : prompts) {
            int width = font.width(prompt.key) + 12;
            boolean drawn = false;
            if (smartRenderer != null) {
                try { smartRenderer.invoke(null, gui, x, 0, prompt.key, prompt.pressed); drawn = true; }
                catch (ReflectiveOperationException exception) { smartRenderer = null; }
            }
            if (!drawn) {
                gui.fill(x, 0, x + width, 13, prompt.pressed ? 0xCC9BABB5 : 0xAA25313A);
                gui.drawCenteredString(font, prompt.key, x + width / 2, 3, 0xFFF3F7F8);
            }
            var description = Component.translatable(prompt.description);
            gui.drawString(font, description, x + width + 6, 3, 0xFFF3F7F8, false);
            x += width + font.width(description) + 22;
        }
        gui.pose().popPose();
    }
    private InventoryPromptRenderer() {}
}
