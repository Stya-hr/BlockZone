package dev.stya.blockzone.map.battlezone;

import dev.stya.blockzone.BlockZone;
import dev.stya.blockzone.net.battlezone.BattlezoneNetwork;
import dev.stya.blockzone.net.battlezone.FlightStateS2CPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BlockZone.MOD_ID)
public final class FlightPoseTrackingEvents {
    private FlightPoseTrackingEvents() { }

    @SubscribeEvent
    public static void startTracking(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer observer && event.getTarget() instanceof ServerPlayer target) {
            int state = BattlezoneNetwork.flightState(target);
            BattlezoneNetwork.sendFlightStateTo(observer, new FlightStateS2CPacket(target.getUUID(), state));
        }
    }
}
