package dev.stya.blockzone.map.battlezone;

import dev.stya.blockzone.BlockZone;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BlockZone.MOD_ID)
public final class CombatEvents {
    private CombatEvents() { }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void hurt(LivingHurtEvent event) {
        if (event.getAmount() > 0 && event.getEntity() instanceof ServerPlayer player
                && !MatchRegeneration.allowed(player)) {
            MatchRegeneration.map(player).ifPresent(map -> map.combatHurt(player));
        }
    }

    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer player) {
            MatchRegeneration.map(player).ifPresent(map -> {
                map.tickRecovery(player);
                if (!MatchRegeneration.allowed(player) && player.getAbsorptionAmount() > map.getMaxCombatArmor(player))
                    player.setAbsorptionAmount(map.getMaxCombatArmor(player));
            });
            boolean hud = MatchRegeneration.map(player).filter(BattlezoneMap::isMatchActive)
                    .filter(map -> player.serverLevel() == map.getServerLevel())
                    .flatMap(map -> map.getMapTeams().getTeamByPlayer(player))
                    .filter(team -> !team.isSpectator()).isPresent();
            dev.stya.blockzone.net.battlezone.BattlezoneNetwork.syncCombat(player, hud, MatchRegeneration.map(player)
                    .map(BattlezoneMap::getArmorPlatePoints).orElse(CombatRecovery.PLATE_POINTS), dev.stya.blockzone.equipment.PlateCapacity.slots(player));
        }
    }
}
