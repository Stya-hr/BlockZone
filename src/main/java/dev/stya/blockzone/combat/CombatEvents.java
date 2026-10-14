package dev.stya.blockzone.combat;

import dev.stya.blockzone.BlockZone;
import dev.stya.blockzone.equipment.PlateCapacity;
import dev.stya.blockzone.map.battlezone.BattlezoneMap;
import dev.stya.blockzone.net.battlezone.BattlezoneNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BlockZone.MOD_ID)
public final class CombatEvents {
    private CombatEvents() { }

    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer player) {
            boolean hud = MatchRegeneration.map(player).filter(BattlezoneMap::isMatchActive)
                    .filter(map -> player.serverLevel() == map.getServerLevel())
                    .flatMap(map -> map.getMapTeams().getTeamByPlayer(player))
                    .filter(team -> !team.isSpectator()).isPresent();
            BattlezoneNetwork.syncCombat(player, hud, MatchRegeneration.map(player)
                    .map(BattlezoneMap::getArmorPlatePoints).orElse(CombatRecovery.PLATE_POINTS), PlateCapacity.slots(player));
        }
    }
}
