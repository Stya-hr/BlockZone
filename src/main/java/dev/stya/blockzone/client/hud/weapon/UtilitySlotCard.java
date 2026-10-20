package dev.stya.blockzone.client.hud.weapon;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;

/** One reusable component for the throwable and both tactical slots. */
final class UtilitySlotCard {
    void render(WeaponHudGraphics g, ItemStack stack, int slot, int x, boolean selected) {
        g.box(x, 28, 66, 66, selected, false);
        g.item(stack, x + 9, 37, 48, 48, false);
        if (!stack.isEmpty()) {
            var mc = Minecraft.getInstance();
            float cooldown = mc.player.getCooldowns().getCooldownPercent(stack.getItem(), mc.getFrameTime());
            String count = Integer.toString(stack.getCount());
            // Item rendering uses a higher Z range. Draw the count above it, without a vanilla decoration pass.
            g.gui.flush(); g.gui.pose().pushPose(); g.gui.pose().translate(0, 0, 300);
            if (cooldown > 0) g.rect(x + 2, 30 + 62 * (1 - cooldown), 62, 62 * cooldown, 0x99000000);
            g.text(count, x + 60 - g.width(count, 16), 74, 16, WeaponHudGraphics.WHITE);
            g.gui.flush(); g.gui.pose().popPose();
        }
        var mc = Minecraft.getInstance();
        var key = WeaponSlotSelection.itemKey(slot);
        int keyWidth = dev.stya.blockzone.client.inventory.InventoryPromptRenderer.keyWidth(mc.font, key.getKey(), 20);
        g.key(key, x + (66 - keyWidth) / 2, 100);
    }
}
