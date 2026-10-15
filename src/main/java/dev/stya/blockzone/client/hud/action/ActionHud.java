package dev.stya.blockzone.client.hud.action;

import com.ptcrys.fpsmatch.common.client.FPSMGameHudManager;
import com.ptcrys.fpsmatch.common.client.screen.hud.IHudRenderer;
import dev.stya.blockzone.BlockZone;
import dev.stya.blockzone.client.battlezone.DeploymentClientController;
import dev.stya.blockzone.client.hud.common.HudCanvas;
import dev.stya.blockzone.client.hud.flight.FlightInstruments;
import dev.stya.blockzone.inventory.PlayerPanelInventory;
import dev.stya.blockzone.map.battlezone.BattlezoneMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/** One actionbar-positioned card arbitrates operation progress and flight prompts. */
@Mod.EventBusSubscriber(modid = BlockZone.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ActionHud implements IHudRenderer {
    private final ActionProgressPanel progress = new ActionProgressPanel();
    private final FlightInstruments flight = new FlightInstruments();
    @SubscribeEvent public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> FPSMGameHudManager.INSTANCE.registerHud(BattlezoneMap.GAME_TYPE, new ActionHud()));
    }
    @Override public void onRenderGuiOverlayPre(RenderGuiOverlayEvent.Pre event) {}
    @Override public void onSpectatorRender(ForgeGui forge, GuiGraphics gui, float partial, int width, int height) {}
    @Override public void onPlayerRender(ForgeGui forge, GuiGraphics gui, float partial, int width, int height) {
        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.options.hideGui || mc.screen != null || !mc.player.isAlive()
                || mc.player.isSpectator() || PlayerPanelInventory.get(mc.player).filter(PlayerPanelInventory::active).isEmpty()) return;
        var action = ActionStatusSource.read(partial);
        boolean airborne = DeploymentClientController.isAirborne();
        if (action == null && !airborne) return;
        try (var g = new HudCanvas(gui)) {
            if (airborne) flight.render(g, partial);
            progress.render(g, action != null ? action : flight.prompt(), g.width / 2, g.height - 300);
        }
    }
}
