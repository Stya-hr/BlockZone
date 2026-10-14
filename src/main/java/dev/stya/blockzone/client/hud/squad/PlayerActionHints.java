package dev.stya.blockzone.client.hud.squad;

import dev.stya.blockzone.client.inventory.InventoryPromptRenderer;
import dev.stya.blockzone.client.inventory.InventorySvgAssets;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import dev.stya.blockzone.registry.BlockzoneItems;

final class PlayerActionHints {
    void render(SquadHudGraphics g, int y) {
        var mc = Minecraft.getInstance();
        int width = InventoryPromptRenderer.renderKey(g.gui, mc.font, mc.options.keyInventory.getKey(), 0, y, 20);
        String label = Component.translatable("hud.blockzone.squad.inventory").getString();
        g.text(label, Math.max(42, width + 8), y + 4, 12, SquadHudStyle.TEXT);
        int iconX = 150;
        // The reference's G is replaced with the actual use binding only when a plate can be used.
        if (mc.player != null && mc.player.getMainHandItem().is(BlockzoneItems.ARMOR_PLATE.get())) {
            int keyWidth = InventoryPromptRenderer.renderKey(g.gui, mc.font, mc.options.keyUse.getKey(), 124, y, 20);
            iconX = Math.max(iconX, 124 + keyWidth + 6);
        }
        // Same 40x40 vector plate silhouette as the reference and the existing inventory.
        float pixelScale = Math.min(mc.getWindow().getWidth() / 2560f, mc.getWindow().getHeight() / 1440f);
        int raster = Math.max(16, Math.round(40 * pixelScale));
        var icon = InventorySvgAssets.get("armor_plates", raster, raster);
        g.gui.blit(icon.id(), iconX, y - 10, 40, 40, 0, 0, icon.width(), icon.height(), icon.width(), icon.height());
    }
}
