package dev.stya.blockzone.client.battlezone;

import dev.stya.blockzone.BlockZone;
import dev.stya.blockzone.client.hud.squad.SquadClientState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Match scope for vanilla overlays; vitals are rendered by the FPSMatch squad HUD. */
@Mod.EventBusSubscriber(modid = BlockZone.MOD_ID, value = Dist.CLIENT)
public final class CombatHud {
    private static boolean active;
    private CombatHud() {}
    public static void setActive(boolean value) {
        active = value;
        if (!value) SquadClientState.clear();
    }
    public static void setCombatState(boolean value, float points, int slots) { setActive(value); }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { setActive(false); }
    @SubscribeEvent public static void reset(com.ptcrys.fpsmatch.common.client.event.FPSMClientResetEvent event) { setActive(false); }
    @SubscribeEvent public static void hideVanillaBars(RenderGuiOverlayEvent.Pre event) {
        if (!active || !com.ptcrys.fpsmatch.common.client.FPSMGameHudManager.enable
                || SquadClientState.members().isEmpty()) return;
        var id = event.getOverlay().id();
        if (id.equals(VanillaGuiOverlay.PLAYER_HEALTH.id())
                || id.equals(VanillaGuiOverlay.ARMOR_LEVEL.id())
                || id.equals(VanillaGuiOverlay.FOOD_LEVEL.id())) event.setCanceled(true);
    }
}
