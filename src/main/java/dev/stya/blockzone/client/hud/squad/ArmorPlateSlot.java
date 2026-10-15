package dev.stya.blockzone.client.hud.squad;

import dev.stya.blockzone.client.equipment.QuickPlating;
import dev.stya.blockzone.client.inventory.InventoryPromptRenderer;
import dev.stya.blockzone.inventory.InventorySlot;
import dev.stya.blockzone.inventory.PlayerPanelInventory;
import net.minecraft.client.Minecraft;

/** Local-only armor plate inventory, kept separate from the synchronized teammate vitals. */
final class ArmorPlateSlot {
    static final int SIZE = 48;
    void render(SquadHudGraphics g, boolean plating, int x, int y) {
        var mc = Minecraft.getInstance();
        var inventory = PlayerPanelInventory.get(mc.player).filter(PlayerPanelInventory::active).orElse(null);
        if (inventory == null) return;
        var stack = inventory.getStack(InventorySlot.ARMOR_PLATES);
        int count = stack.getCount();
        int border = plating ? SquadHudStyle.TEXT : 0x73666666;
        int thickness = plating ? 3 : 2;
        g.rect(x, y, SIZE, SIZE, 0x33000000);
        g.rect(x, y, SIZE, thickness, border); g.rect(x, y + SIZE - thickness, SIZE, thickness, border);
        g.rect(x, y + thickness, thickness, SIZE - 2 * thickness, border);
        g.rect(x + SIZE - thickness, y + thickness, thickness, SIZE - 2 * thickness, border);
        if (!stack.isEmpty()) {
            g.gui.flush(); g.gui.pose().pushPose();
            g.gui.pose().translate(x + 6, y + 6, 0); g.gui.pose().scale((SIZE - 12) / 16f, (SIZE - 12) / 16f, 1);
            g.gui.renderItem(stack, 0, 0);
            g.gui.flush(); g.gui.pose().popPose();
        }
        String amount = Integer.toString(count);
        float width = mc.font.width(amount) * 1.5f;
        // Native item rendering uses a higher Z range; keep the count above the item.
        g.gui.pose().pushPose(); g.gui.pose().translate(0, 0, 300);
        g.text(amount, x + SIZE - 3 - width, y + SIZE - 15, 12, count == 0 ? SquadHudStyle.MUTED : SquadHudStyle.TEXT);
        g.gui.flush(); g.gui.pose().popPose();
        if (!QuickPlating.KEY.isUnbound()) InventoryPromptRenderer.renderKey(g.gui, mc.font, QuickPlating.KEY.getKey(), x + (SIZE - 20) / 2, y + SIZE + 6, 20);
    }
}
