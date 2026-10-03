package dev.stya.blockzone.map.battlezone;

import dev.stya.blockzone.BlockZone;
import com.ptcrys.fpsmatch.common.event.register.RegisterFPSMapEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BlockZone.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class BattlezoneMapRegistration {
    private BattlezoneMapRegistration() { }

    @SubscribeEvent
    public static void registerGameType(RegisterFPSMapEvent event) {
        event.registerGameType(BattlezoneMap.GAME_TYPE, BattlezoneMap::new);
    }
}
