package dev.stya.blockzone.squad;

import com.tacz.guns.api.event.common.GunFireEvent;
import dev.stya.blockzone.BlockZone;
import dev.stya.blockzone.combat.MatchRegeneration;
import dev.stya.blockzone.map.battlezone.BattlezoneMap;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;

/** Accepted server-side TaCZ shots, visible only through the shooter's squad snapshot. */
@Mod.EventBusSubscriber(modid = BlockZone.MOD_ID)
public final class SquadGunActivity {
    private record Shot(BattlezoneMap map, long expires) {}
    private static final Map<ServerPlayer, Shot> SHOTS = new WeakHashMap<>();
    private SquadGunActivity() {}
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void fired(GunFireEvent event) {
        if (event.getLogicalSide() != LogicalSide.SERVER || !(event.getShooter() instanceof ServerPlayer player)) return;
        MatchRegeneration.map(player).filter(BattlezoneMap::isMatchActive).ifPresent(map ->
                SHOTS.put(player, new Shot(map, player.serverLevel().getGameTime() + 6)));
    }
    static boolean isFiring(ServerPlayer player, BattlezoneMap map) {
        var shot = SHOTS.get(player);
        return shot != null && shot.map == map && player.serverLevel().getGameTime() < shot.expires;
    }
    static void clear(ServerPlayer player) { SHOTS.remove(player); }
}
