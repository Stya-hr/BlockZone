package dev.stya.blockzone.client.battlezone;

import dev.stya.blockzone.BlockZone;
import dev.stya.blockzone.map.battlezone.CombatRecovery;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BlockZone.MOD_ID, value = Dist.CLIENT)
public final class CombatHud {
    private CombatHud() { }

    @SubscribeEvent
    public static void render(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.player.isSpectator() || mc.options.hideGui
                || ZoneClientState.current(0) == null) return;
        int x = 12;
        int y = event.getGuiGraphics().guiHeight() - 48;
        float armor = mc.player.getAbsorptionAmount();
        for (int i = 0; i < 3; i++) {
            int left = x + i * 34;
            event.getGuiGraphics().fill(left, y, left + 30, y + 6, 0xB0303945);
            int width = Mth.ceil(30 * Mth.clamp((armor - i * CombatRecovery.PLATE_POINTS)
                    / CombatRecovery.PLATE_POINTS, 0, 1));
            event.getGuiGraphics().fill(left, y, left + width, y + 6, 0xFF55B9F3);
        }
        if (mc.player.isUsingItem() && mc.player.getUseItem().is(dev.stya.blockzone.loot.LootCrateRegistry.ARMOR_PLATE.get())) {
            int width = Mth.clamp(mc.player.getTicksUsingItem() * 98 / 40, 0, 98);
            event.getGuiGraphics().fill(x, y + 10, x + 98, y + 13, 0xB0303945);
            event.getGuiGraphics().fill(x, y + 10, x + width, y + 13, 0xFFE4EDF5);
        }
    }
}
