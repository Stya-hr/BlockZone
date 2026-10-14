package dev.stya.blockzone.client.hud.squad;

import dev.stya.blockzone.client.equipment.QuickPlating;
import dev.stya.blockzone.client.inventory.InventoryPromptRenderer;
import dev.stya.blockzone.client.inventory.InventorySvgAssets;
import net.minecraft.client.Minecraft;

final class PlayerActionHints {
    void render(SquadHudGraphics g, int y) {
        var mc = Minecraft.getInstance();
        int width = InventoryPromptRenderer.renderKey(g.gui, mc.font, mc.options.keyInventory.getKey(), 0, y, 20);
        int backpackX = Math.max(40, width + 6);
        icon(g, "backpack", backpackX, y - 10);
        int plateKeyX = Math.max(124, backpackX + 40 + 16);
        int keyWidth = InventoryPromptRenderer.renderKey(g.gui, mc.font, QuickPlating.KEY.getKey(), plateKeyX, y, 20);
        icon(g, "armor_plates", plateKeyX + keyWidth + 6, y - 10);
    }
    private static void icon(SquadHudGraphics g, String name, int x, int y) {
        var mc = Minecraft.getInstance();
        float pixelScale = Math.min(mc.getWindow().getWidth() / 2560f, mc.getWindow().getHeight() / 1440f);
        int raster = Math.max(16, Math.round(40 * pixelScale));
        var texture = InventorySvgAssets.get(name, raster, raster);
        g.gui.flush();
        g.gui.blit(texture.id(), x, y, 40, 40, 0, 0, texture.width(), texture.height(), texture.width(), texture.height());
    }
}
