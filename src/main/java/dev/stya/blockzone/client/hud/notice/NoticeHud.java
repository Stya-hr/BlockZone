package dev.stya.blockzone.client.hud.notice;

import com.ptcrys.fpsmatch.common.client.FPSMGameHudManager;
import com.ptcrys.fpsmatch.common.client.screen.hud.IHudRenderer;
import dev.stya.blockzone.BlockZone;
import dev.stya.blockzone.client.hud.common.HudCanvas;
import dev.stya.blockzone.client.ui.ModernUiLayer;
import dev.stya.blockzone.map.battlezone.BattlezoneMap;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = BlockZone.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class NoticeHud implements IHudRenderer {
    private final AnnouncementBanner banner = new AnnouncementBanner();
    private final ActionGuidancePanel guidance = new ActionGuidancePanel();
    @SubscribeEvent public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> FPSMGameHudManager.INSTANCE.registerHud(BattlezoneMap.GAME_TYPE, new NoticeHud()));
    }
    @Override public void onRenderGuiOverlayPre(RenderGuiOverlayEvent.Pre event) {}
    @Override public void onSpectatorRender(ForgeGui forge, GuiGraphics gui, float partial, int width, int height) {
        onPlayerRender(forge, gui, partial, width, height);
    }
    @Override public void onPlayerRender(ForgeGui forge, GuiGraphics gui, float partial, int width, int height) {
        if (!ModernUiLayer.hudVisible()) return;
        try (var g = new HudCanvas(ModernUiLayer.hud())) {
            guidance.render(g, guidance.read(partial));
            banner.render(g, NoticeClientState.announcement(), NoticeClientState.opacity());
        }
    }
}
