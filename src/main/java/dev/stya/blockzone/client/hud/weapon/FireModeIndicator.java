package dev.stya.blockzone.client.hud.weapon;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.stya.blockzone.equipment.WeaponAmmo;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.ModList;

final class FireModeIndicator {
    void render(WeaponHudGraphics g, WeaponAmmo.View ammo) {
        if (ammo == null || ammo.fireMode().isEmpty() || !ModList.get().isLoaded("tacz")) return;
        g.key(com.tacz.guns.client.input.FireSelectKey.FIRE_SELECT_KEY, 444, 0);
        String mode = switch (ammo.fireMode()) { case "auto" -> "auto"; case "burst" -> "burst"; default -> "semi"; };
        var texture = ResourceLocation.fromNamespaceAndPath("tacz", "textures/hud/fire_mode_" + mode + ".png");
        g.gui.flush(); RenderSystem.enableBlend(); RenderSystem.defaultBlendFunc();
        g.gui.blit(texture, 470, 0, 20, 20, 0, 0, 10, 10, 10, 10);
        g.gui.flush(); RenderSystem.disableBlend();
    }
}
