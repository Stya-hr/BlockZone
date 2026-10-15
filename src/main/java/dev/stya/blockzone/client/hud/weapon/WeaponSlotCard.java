package dev.stya.blockzone.client.hud.weapon;

import dev.stya.blockzone.equipment.WeaponAmmo;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;

final class WeaponSlotCard {
    private final AmmoReadout readout = new AmmoReadout();
    void render(WeaponHudGraphics g, ItemStack stack, WeaponAmmo.View ammo, int slot, boolean large, boolean selected) {
        int x = large ? 231 : 293, y = large ? 28 : 98, w = large ? 262 : 200, h = large ? 65 : 40;
        g.box(x, y, w, h, selected, true);
        g.item(stack, x + 10, large ? 32 : 101, large ? 156 : 94, large ? 56 : 34, true);
        readout.render(g, ammo, x + w - 11, y + (large ? 14 : 10), large);
        if (!selected) g.key(Minecraft.getInstance().options.keyHotbarSlots[slot], large ? x : x - 28, large ? 0 : 108);
    }
}
