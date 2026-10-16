package dev.stya.blockzone.client.hud.weapon;

import dev.stya.blockzone.equipment.WeaponAmmo;
import dev.stya.blockzone.inventory.InventorySlot;
import dev.stya.blockzone.inventory.PlayerPanelInventory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/** The SVG's right/bottom anchors and proportions share a single aspect-preserving transform. */
public final class WeaponStatusPanel {
    private final WeaponSlotCard weapon = new WeaponSlotCard();
    private final UtilitySlotCard utility = new UtilitySlotCard();
    private final FireModeIndicator fireMode = new FireModeIndicator();
    private java.util.UUID owner;
    private java.util.UUID round;
    private int prominent;
    public void render(GuiGraphics gui, PlayerPanelInventory inventory) {
        var mc = Minecraft.getInstance();
        var player = mc.player;
        if (!java.util.Objects.equals(owner, player.getUUID()) || !java.util.Objects.equals(round, inventory.round())) {
            owner = player.getUUID(); round = inventory.round();
            prominent = inventory.getStack(InventorySlot.PRIMARY_WEAPON).isEmpty()
                    && !inventory.getStack(InventorySlot.SECONDARY_WEAPON).isEmpty() ? 1 : 0;
        }
        int selected = player.getInventory().selected;
        if (selected < 2) prominent = selected;
        var main = inventory.getStack(InventorySlot.WEAPONS.get(prominent));
        var other = inventory.getStack(InventorySlot.WEAPONS.get(1 - prominent));
        var mainAmmo = WeaponAmmo.read(player, inventory, main);
        var window = mc.getWindow();
        float scale = dev.stya.blockzone.client.hud.common.HudCanvas.scale() * 1.05f;
        gui.pose().pushPose();
        try {
            gui.pose().translate(window.getWidth() / window.getGuiScale() - 536 * scale,
                    window.getHeight() / window.getGuiScale() - 221 * scale, 0);
            gui.pose().scale(scale, scale, 1);
            var g = new WeaponHudGraphics(gui);
            weapon.render(g, main, mainAmmo, prominent, true, selected == prominent);
            weapon.render(g, other, WeaponAmmo.read(player, inventory, other), 1 - prominent, false, false);
            for (int i = 0; i < 3; i++) utility.render(g, inventory.getStack(InventorySlot.ITEMS.get(i)), i + 2, i * 70, selected == i + 2);
            if (selected == prominent) fireMode.render(g, mainAmmo);
            gui.flush();
        } finally { gui.pose().popPose(); }
    }
}
