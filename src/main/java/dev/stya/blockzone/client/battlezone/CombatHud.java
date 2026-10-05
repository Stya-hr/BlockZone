package dev.stya.blockzone.client.battlezone;

import dev.stya.blockzone.BlockZone;
import dev.stya.blockzone.map.battlezone.CombatRecovery;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BlockZone.MOD_ID, value = Dist.CLIENT)
public final class CombatHud {
    private static boolean active;
    private static int plateSlots = 2;
    private static float platePoints = CombatRecovery.PLATE_POINTS;
    private CombatHud() { }

    public static void setActive(boolean value) { active = value; }

    public static void setCombatState(boolean value, float points, int slots) {
        active = value;
        platePoints = points;
        plateSlots = Mth.clamp(slots, 0, 3);
    }

    @SubscribeEvent
    public static void hideVanillaBars(RenderGuiOverlayEvent.Pre event) {
        if (!active) return;
        var id = event.getOverlay().id();
        if (id.equals(VanillaGuiOverlay.PLAYER_HEALTH.id())
                || id.equals(VanillaGuiOverlay.ARMOR_LEVEL.id())
                || id.equals(VanillaGuiOverlay.FOOD_LEVEL.id())) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void render(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (!active || mc.player == null || mc.player.isSpectator() || mc.options.hideGui) return;
        var gui = event.getGuiGraphics();
        int x = 12;
        int y = gui.guiHeight() - 72;
        int width = 180;
        float health = mc.player.getHealth();
        float maxHealth = mc.player.getMaxHealth();
        float armor = mc.player.getAbsorptionAmount();
        int healthColor = health <= maxHealth * .3F ? 0xFFFF6868 : 0xFFE7EEF4;
        gui.fill(x - 5, y - 5, x + width + 5, y + 47, 0xB018202A);
        gui.fill(x - 5, y - 5, x - 3, y + 47, mc.player.hurtTime > 0 ? 0xFFFF6868 : 0xFF55B9F3);
        gui.drawString(mc.font, Component.translatable("hud.blockzone.health"), x, y, 0xFFB8C3CD, false);
        String hp = Mth.ceil(health) + " / " + Mth.ceil(maxHealth);
        gui.drawString(mc.font, hp, x + width - mc.font.width(hp), y, healthColor, false);
        gui.fill(x, y + 12, x + width, y + 18, 0xFF394450);
        gui.fill(x, y + 12, x + Mth.ceil(width * Mth.clamp(health / maxHealth, 0, 1)), y + 18, healthColor);
        gui.drawString(mc.font, Component.translatable("hud.blockzone.armor"), x, y + 24, 0xFFB8C3CD, false);
        String ap = Mth.ceil(armor) + " / " + Mth.ceil((platePoints * plateSlots));
        gui.drawString(mc.font, ap, x + width - mc.font.width(ap), y + 24, 0xFF55B9F3, false);
        for (int i = 0; i < plateSlots; i++) {
            int segment = (width - (plateSlots - 1) * 3) / plateSlots;
            int left = x + i * (segment + 3);
            gui.fill(left, y + 36, left + segment, y + 42, 0xFF394450);
            int filled = Mth.ceil(segment * Mth.clamp((armor - i * platePoints)
                    / platePoints, 0, 1));
            gui.fill(left, y + 36, left + filled, y + 42, 0xFF55B9F3);
        }
        if (mc.player.isUsingItem() && mc.player.getUseItem().is(dev.stya.blockzone.equipment.EquipmentRegistry.ARMOR_PLATE.get())) {
            gui.fill(x, y - 18, x + width, y - 8, 0xB018202A);
            int filled = Mth.clamp(mc.player.getTicksUsingItem() * width / 40, 0, width);
            gui.fill(x, y - 10, x + filled, y - 8, 0xFF55B9F3);
            gui.drawString(mc.font, Component.translatable("hud.blockzone.plating"), x + 2, y - 18, 0xFFE7EEF4, false);
        }
    }
}
