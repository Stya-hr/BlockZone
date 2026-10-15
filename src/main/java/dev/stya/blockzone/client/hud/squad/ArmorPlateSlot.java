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
    void render(SquadHudGraphics g, boolean plating, int x) {
        var mc = Minecraft.getInstance();
        var inventory = PlayerPanelInventory.get(mc.player).filter(PlayerPanelInventory::active).orElse(null);
        if (inventory == null) return;
        int count = inventory.getStack(InventorySlot.ARMOR_PLATES).getCount();
        int border = plating ? SquadHudStyle.TEXT : 0x73666666;
        g.rect(x, 0, 64, 64, 0x33000000);
        g.rect(x, 0, 64, 1, border); g.rect(x, 63, 64, 1, border);
        g.rect(x, 1, 1, 62, border); g.rect(x + 63, 1, 1, 62, border);
        var window = mc.getWindow();
        float scale = Math.min(window.getWidth() / 2560f, window.getHeight() / 1440f);
        var icon = InventorySvgAssets.get("plates", Math.max(1, (int)Math.ceil(40 * scale)), Math.max(1, (int)Math.ceil(42 * scale)));
        g.gui.flush(); g.gui.setColor(1, 1, 1, count == 0 ? .25f : 1);
        RenderSystem.enableBlend(); RenderSystem.defaultBlendFunc();
        g.gui.blit(icon.id(), x + 12, 7, 40, 42, 0, 0, icon.width(), icon.height(), icon.width(), icon.height());
        g.gui.flush(); g.gui.setColor(1, 1, 1, 1); RenderSystem.disableBlend();
        String amount = Integer.toString(count);
        float width = mc.font.width(amount) * 2f;
        g.text(amount, x + 59 - width, 44, 16, count == 0 ? SquadHudStyle.MUTED : SquadHudStyle.TEXT);
        if (!QuickPlating.KEY.isUnbound()) InventoryPromptRenderer.renderKey(g.gui, mc.font, QuickPlating.KEY.getKey(), x + 22, 70, 20);
    }
}
