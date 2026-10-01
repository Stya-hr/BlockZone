package dev.stya.blockzone.game.battlezone;

import dev.stya.blockzone.BlockZone;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BlockZone.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class BattlezoneWorldRendering {
    private BattlezoneWorldRendering() {
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        BattlezoneClientRendering.renderWorld(event);
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        BattlezoneClientState.clear();
    }
}
