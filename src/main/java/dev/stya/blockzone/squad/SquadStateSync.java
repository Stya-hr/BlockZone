package dev.stya.blockzone.squad;

import com.ptcrys.fpsmatch.core.team.ServerTeam;
import dev.stya.blockzone.combat.CombatStateSync;
import dev.stya.blockzone.equipment.PlateCapacity;
import dev.stya.blockzone.map.battlezone.BattlezoneMap;
import dev.stya.blockzone.net.BlockzoneNetwork;
import dev.stya.blockzone.net.battlezone.SquadStateS2CPacket;
import dev.stya.blockzone.registry.BlockzoneItems;
import java.util.*;
import net.minecraft.server.level.ServerPlayer;

/** FPSMatch owns membership; BlockZone supplies vitals not present in its team packets. */
public final class SquadStateSync {
    private static final Map<ServerPlayer, SquadStateS2CPacket> SENT = new WeakHashMap<>();
    private static final Map<ServerTeam, SquadMemberNumbers> NUMBERS = new WeakHashMap<>();
    private SquadStateSync() {}

    public static SquadStateS2CPacket capture(BattlezoneMap map, ServerTeam team) {
        if (!map.isMatchActive() || team.isSpectator()) return SquadStateS2CPacket.EMPTY;
        var numbers = NUMBERS.computeIfAbsent(team, ignored -> new SquadMemberNumbers());
        numbers.update(team.getPlayerList());
        var roster = team.getPlayersData().stream().sorted(Comparator.comparingInt(p -> numbers.number(p.getOwner()))).toList();
        var members = new ArrayList<SquadMemberStatus>();
        for (int i = 0; i < roster.size(); i++) {
            var data = roster.get(i);
            var player = map.getServerLevel().getServer().getPlayerList().getPlayer(data.getOwner());
            var rescue = player == null ? null : CombatStateSync.rescuePacket(player);
            float maxHealth = player == null ? map.getCombatHealth() : player.getMaxHealth();
            // DownedController initializes a separate downed health pool (usually 50, not 100).
            // Send its actual configured capacity so the red bar starts full on every client.
            if (rescue != null && rescue.state() == 1) {
                maxHealth = Math.min(maxHealth, (float)map.getCombatConfig().downedHealth());
            }
            members.add(new SquadMemberStatus(data.getOwner(), data.name().getString(), numbers.number(data.getOwner()), player != null, data.isLiving(),
                    player == null ? 0 : player.getHealth(), maxHealth,
                    player == null ? 0 : player.getAbsorptionAmount(), map.getArmorPlatePoints(), player == null ? 0 : PlateCapacity.slots(player),
                    rescue == null ? 0 : rescue.state(), rescue == null ? 0 : rescue.bleedTicks(), rescue == null ? 0 : rescue.bleedTotalTicks(),
                    rescue == null ? 0 : rescue.rescueTicks(), rescue == null ? 0 : rescue.rescueTotalTicks(),
                    player != null && player.isUsingItem() && player.getUseItem().is(BlockzoneItems.ARMOR_PLATE.get()),
                    player != null && data.isLiving() && SquadGunActivity.isFiring(player, map)));
        }
        return new SquadStateS2CPacket(map.getMapName(), team.getName(), members);
    }
    public static void tick(BattlezoneMap map) {
        if (map.getServerLevel().getGameTime() % 5 != 0) return;
        for (var team : map.getMapTeams().getNormalTeams()) {
            var packet = capture(map, team);
            for (var player : team.getOnline()) {
                if (packet.equals(SENT.get(player))) continue;
                SENT.put(player, packet);
                BlockzoneNetwork.sendToPlayer(player, packet);
            }
        }
    }
    public static void clear(ServerPlayer player) {
        SENT.remove(player);
        SquadGunActivity.clear(player);
        BlockzoneNetwork.sendToPlayer(player, SquadStateS2CPacket.EMPTY);
    }
}
