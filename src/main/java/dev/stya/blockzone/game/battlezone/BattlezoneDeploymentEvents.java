package dev.stya.blockzone.game.battlezone;

import com.ptcrys.fpsmatch.core.FPSMCore;
import dev.stya.blockzone.BlockZone;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BlockZone.MOD_ID)
public final class BattlezoneDeploymentEvents {
    private BattlezoneDeploymentEvents() { }

    @SubscribeEvent(priority = net.minecraftforge.eventbus.api.EventPriority.HIGHEST)
    public static void logout(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            FPSMCore.getInstance().getMapByClass(BattlezoneMap.class)
                    .forEach(map -> map.clearAirbornePlayer(player));
        }
    }

    @SubscribeEvent
    public static void tickPlayer(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer player) {
            FPSMCore.getInstance().getMapByPlayerWithSpec(player)
                    .filter(BattlezoneMap.class::isInstance).map(BattlezoneMap.class::cast)
                    .ifPresent(map -> map.tickDeploymentPlayer(player));
        }
    }

    @SubscribeEvent
    public static void protectLanding(LivingFallEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            FPSMCore.getInstance().getMapByPlayerWithSpec(player)
                    .filter(BattlezoneMap.class::isInstance).map(BattlezoneMap.class::cast)
                    .filter(map -> map.hasDeploymentProtection(player))
                    .ifPresent(map -> {
                        event.setCanceled(true);
                        player.fallDistance = 0;
                    });
        }
    }
}
