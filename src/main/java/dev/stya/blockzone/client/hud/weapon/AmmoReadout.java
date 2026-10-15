package dev.stya.blockzone.client.hud.weapon;

import dev.stya.blockzone.equipment.WeaponAmmo;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

final class AmmoReadout {
    void render(WeaponHudGraphics g, WeaponAmmo.View ammo, int right, int y, boolean large) {
        if (ammo == null) return;
        float size = large ? 40 : 20, reserveSize = 20;
        var reserve = Component.literal("/" + (ammo.unlimited() ? "INF" : ammo.reserve())).withStyle(ChatFormatting.BOLD);
        float reserveWidth = g.width(reserve, reserveSize);
        var loaded = Component.literal(Integer.toString(ammo.loaded())).withStyle(ChatFormatting.BOLD);
        float total = reserveWidth + g.width(loaded, size);
        // Keep long counters inside their own region without overlapping the gun silhouette.
        float fit = Math.min(1, 84f / Math.max(1, total));
        g.text(loaded, right - total * fit, y, size * fit, ammo.loaded() == 0 ? WeaponHudGraphics.RED : WeaponHudGraphics.WHITE);
        g.text(reserve, right - reserveWidth * fit, y, reserveSize * fit, WeaponHudGraphics.WHITE);
    }
}
