package dev.stya.blockzone.client.hud.weapon;

import com.ptcrys.fpsmatch.common.client.FPSMClient;
import com.ptcrys.fpsmatch.common.client.FPSMGameHudManager;
import com.ptcrys.fpsmatch.common.client.screen.hud.IHudRenderer;
import dev.stya.blockzone.BlockZone;
import dev.stya.blockzone.inventory.PlayerPanelInventory;
import dev.stya.blockzone.map.battlezone.BattlezoneMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/** Only replaces the local player's match hotbar; outside matches the vanilla UI remains intact. */
@Mod.EventBusSubscriber(modid = BlockZone.MOD_ID, value = Dist.CLIENT)
public final class WeaponHud implements IHudRenderer {
    private final WeaponStatusPanel panel = new WeaponStatusPanel();
    static PlayerPanelInventory inventory() {
        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.player.isSpectator() || !mc.player.isAlive() || !FPSMGameHudManager.shouldRender()
                || FPSMClient.getGlobalData().isSpectator()
                || !BattlezoneMap.GAME_TYPE.equals(FPSMClient.getGlobalData().getCurrentGameType())) return null;
        return PlayerPanelInventory.get(mc.player).filter(PlayerPanelInventory::active).orElse(null);
    }
    @SubscribeEvent public static void replaceHotbar(RenderGuiOverlayEvent.Pre event) {
        if (inventory() == null) return;
        if (event.getOverlay() == VanillaGuiOverlay.HOTBAR.type() || event.getOverlay() == VanillaGuiOverlay.ITEM_NAME.type()
                || event.getOverlay() == VanillaGuiOverlay.EXPERIENCE_BAR.type()
                || event.getOverlay().id().toString().equals("tacz:tac_gun_hud_overlay")) event.setCanceled(true);
    }
    @Override public void onRenderGuiOverlayPre(RenderGuiOverlayEvent.Pre event) {}
    @Override public void onSpectatorRender(ForgeGui forge, GuiGraphics gui, float partial, int width, int height) {}
    @Override public void onPlayerRender(ForgeGui forge, GuiGraphics gui, float partial, int width, int height) {
        var mc = Minecraft.getInstance();
        var data = inventory();
        if (data != null && !mc.options.hideGui && mc.screen == null) panel.render(gui, data);
    }
    @Mod.EventBusSubscriber(modid = BlockZone.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        @SubscribeEvent public static void setup(FMLClientSetupEvent event) {
            event.enqueueWork(() -> FPSMGameHudManager.INSTANCE.registerHud(BattlezoneMap.GAME_TYPE, new WeaponHud()));
        }
    }
}
