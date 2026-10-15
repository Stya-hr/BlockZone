package dev.stya.blockzone.client.hud.weapon;

import dev.stya.blockzone.equipment.WeaponAmmo;

final class AmmoReadout {
    void render(WeaponHudGraphics g, WeaponAmmo.View ammo, int right, int y, boolean large) {
        if (ammo == null) return;
        float size = large ? 38 : 18, reserveSize = large ? 20 : 18;
        String reserve = "/" + (ammo.unlimited() ? "INF" : ammo.reserve());
        float reserveWidth = g.width(reserve, reserveSize);
        String loaded = Integer.toString(ammo.loaded());
        float total = reserveWidth + g.width(loaded, size);
        // Keep long counters inside their own region without overlapping the gun silhouette.
        float fit = Math.min(1, (large ? 94f : 112f) / Math.max(1, total));
        g.text(loaded, right - total * fit, y, size * fit, ammo.loaded() == 0 ? WeaponHudGraphics.RED : WeaponHudGraphics.WHITE);
        g.text(reserve, right - reserveWidth * fit, y + (size - reserveSize) * fit / 2, reserveSize * fit, WeaponHudGraphics.WHITE);
    }
}
