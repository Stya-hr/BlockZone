package dev.stya.blockzone.client.hud.weapon;

import dev.stya.blockzone.equipment.WeaponAmmo;
import net.minecraftforge.fml.ModList;

final class FireModeIndicator {
    void render(WeaponHudGraphics g, WeaponAmmo.View ammo) {
        if (ammo == null || ammo.fireMode().isEmpty() || !ModList.get().isLoaded("tacz")) return;
        g.key(com.tacz.guns.client.input.FireSelectKey.FIRE_SELECT_KEY, 444, 0);
        int bars = switch (ammo.fireMode()) { case "auto" -> 3; case "burst" -> 2; default -> 1; };
        for (int i = 0; i < bars; i++) {
            float x = 470 + i * (20f / 3);
            g.rect(x, -4, 16f / 3, 16f / 3, WeaponHudGraphics.WHITE);
            g.rect(x, 8f / 3, 16f / 3, 52f / 3, WeaponHudGraphics.WHITE);
        }
    }
}
