package dev.stya.blockzone.client.hud.squad;

import com.ptcrys.fpsmatch.common.client.FPSMGameHudManager;
import com.ptcrys.fpsmatch.common.client.screen.hud.IHudRenderer;
import dev.stya.blockzone.BlockZone;
import dev.stya.blockzone.map.battlezone.BattlezoneMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/** Registered with FPSMatch's game-type HUD lifecycle, including eliminated teammates. */
@Mod.EventBusSubscriber(modid = BlockZone.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class SquadHud implements IHudRenderer {
    private final SquadStatusPanel panel = new SquadStatusPanel();
    @SubscribeEvent public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> FPSMGameHudManager.INSTANCE.registerHud(BattlezoneMap.GAME_TYPE, new SquadHud()));
    }
    @Override public void onRenderGuiOverlayPre(RenderGuiOverlayEvent.Pre event) {}
    @Override public void onSpectatorRender(ForgeGui forge, GuiGraphics gui, float partial, int width, int height) {
        onPlayerRender(forge, gui, partial, width, height);
    }
    @Override public void onPlayerRender(ForgeGui forge, GuiGraphics gui, float partial, int width, int height) {
        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || mc.screen != null) return;
        panel.render(dev.stya.blockzone.client.ui.ModernUiLayer.hud(), SquadClientState.members(), mc.player.getUUID());
    }
}
