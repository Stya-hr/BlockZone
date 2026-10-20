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

/** Operation progress stays at the actionbar; contextual actions stay below the crosshair. */
@Mod.EventBusSubscriber(modid = BlockZone.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ActionHud implements IHudRenderer {
    private final ActionProgressPanel progress = new ActionProgressPanel();
    private final FlightInstruments flight = new FlightInstruments();
    private final ContextActionPrompt context = new ContextActionPrompt();
    private final GroundItemPanel loot = new GroundItemPanel();
    @SubscribeEvent public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> FPSMGameHudManager.INSTANCE.registerHud(BattlezoneMap.GAME_TYPE, new ActionHud()));
    }
    @Override public void onRenderGuiOverlayPre(RenderGuiOverlayEvent.Pre event) {}
    @Override public void onSpectatorRender(ForgeGui forge, GuiGraphics gui, float partial, int width, int height) {}
    @Override public void onPlayerRender(ForgeGui forge, GuiGraphics gui, float partial, int width, int height) {
        var mc = Minecraft.getInstance();
        if (!dev.stya.blockzone.client.ui.ModernUiLayer.hudVisible() || mc.level == null || !mc.player.isAlive()
                || mc.player.isSpectator() || PlayerPanelInventory.get(mc.player).filter(PlayerPanelInventory::active).isEmpty()) return;
        var action = ActionStatusSource.read(partial);
        var drop = dev.stya.blockzone.client.loot.DropTargeting.target();
        boolean airborne = DeploymentClientController.isAirborne();
        var feedback = dev.stya.blockzone.client.hud.notice.NoticeClientState.actionFeedback();
        if (action == null && drop == null && !airborne && feedback == null) return;
        try (var g = new HudCanvas(dev.stya.blockzone.client.ui.ModernUiLayer.hud())) {
            if (drop != null) loot.render(g, drop, partial);
            if (DeploymentClientController.state() == 2 || DeploymentClientController.state() == 3) flight.render(g, partial);
            if (action != null && action.progress() != null) progress.render(g, action, g.width / 2, g.height - 300);
            else if (feedback != null) {
                float y = g.height - 300;
                int alpha = Math.round(255 * dev.stya.blockzone.client.hud.notice.NoticeClientState.actionOpacity());
                var title = g.fit(feedback.title(), 388, 20);
                g.textWithShadow(title, (g.width - g.textWidth(title, 20)) / 2, y + 20, 20,
                        (alpha << 24) | (feedback.color() & 0xFFFFFF));
            }
            else if (action != null || airborne) context.render(g, action != null ? action : flight.prompt(), g.width / 2, g.height / 2 + 170);
        }
    }
}
