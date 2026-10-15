package dev.stya.blockzone.client.hud.squad;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.stya.blockzone.client.equipment.QuickPlating;
import dev.stya.blockzone.client.inventory.InventoryPromptRenderer;
import dev.stya.blockzone.client.inventory.InventorySvgAssets;
import dev.stya.blockzone.inventory.InventorySlot;
import dev.stya.blockzone.inventory.PlayerPanelInventory;
import net.minecraft.client.Minecraft;

/** Local-only armor plate inventory, kept separate from the synchronized teammate vitals. */
final class ArmorPlateSlot {
    void render(SquadHudGraphics g, boolean plating, int x, int y) {
        var mc = Minecraft.getInstance();
        var inventory = PlayerPanelInventory.get(mc.player).filter(PlayerPanelInventory::active).orElse(null);
        if (inventory == null) return;
        int count = inventory.getStack(InventorySlot.ARMOR_PLATES).getCount();
        int border = plating ? SquadHudStyle.TEXT : 0x73666666;
        g.rect(x, y, 32, 32, 0x33000000);
        g.rect(x, y, 32, 1, border); g.rect(x, y + 31, 32, 1, border);
        g.rect(x, y + 1, 1, 30, border); g.rect(x + 31, y + 1, 1, 30, border);
        var window = mc.getWindow();
        float scale = Math.min(window.getWidth() / 2560f, window.getHeight() / 1440f);
        var icon = InventorySvgAssets.get("plates", Math.max(1, (int)Math.ceil(20 * scale)), Math.max(1, (int)Math.ceil(21 * scale)));
        g.gui.flush(); g.gui.setColor(1, 1, 1, count == 0 ? .25f : 1);
        RenderSystem.enableBlend(); RenderSystem.defaultBlendFunc();
        g.gui.blit(icon.id(), x + 6, y + 3, 20, 21, 0, 0, icon.width(), icon.height(), icon.width(), icon.height());
        g.gui.flush(); g.gui.setColor(1, 1, 1, 1); RenderSystem.disableBlend();
        String amount = Integer.toString(count);
        float width = mc.font.width(amount) * 1.5f;
        g.rect(x + 27 - width, y + 16, width + 4, 14, 0xAA10181E);
        g.text(amount, x + 29 - width, y + 17, 12, count == 0 ? SquadHudStyle.MUTED : SquadHudStyle.TEXT);
        if (!QuickPlating.KEY.isUnbound()) InventoryPromptRenderer.renderKey(g.gui, mc.font, QuickPlating.KEY.getKey(), x + 6, y + 38, 20);
    }
}
