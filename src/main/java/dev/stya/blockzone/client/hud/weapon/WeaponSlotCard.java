package dev.stya.blockzone.client.hud.weapon;

import dev.stya.blockzone.equipment.WeaponAmmo;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;

final class WeaponSlotCard {
    private final AmmoReadout readout = new AmmoReadout();
    void render(WeaponHudGraphics g, ItemStack stack, WeaponAmmo.View ammo, int slot, boolean large, boolean selected) {
        int x = large ? 231 : 259, y = large ? 28 : 98, w = large ? 262 : 234, h = large ? 66 : 40;
        g.box(x, y, w, h, selected, true);
        g.item(stack, x + (large ? 10 : 12), y + 4, large ? 156 : 114, h - 8, true);
        readout.render(g, ammo, x + w - 8, y + (large ? 12 : 11), large);
        g.key(Minecraft.getInstance().options.keyHotbarSlots[slot], 231, large ? 0 : 108);
    }
}
