package dev.stya.blockzone.client.hud.squad;

import com.ptcrys.fpsmatch.common.client.FPSMClient;
import dev.stya.blockzone.BlockZone;
import dev.stya.blockzone.map.battlezone.BattlezoneMap;
import dev.stya.blockzone.net.battlezone.SquadStateS2CPacket;
import java.util.List;
import dev.stya.blockzone.squad.SquadMemberStatus;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BlockZone.MOD_ID, value = Dist.CLIENT)
public final class SquadClientState {
    private static SquadStateS2CPacket snapshot = SquadStateS2CPacket.EMPTY;
    private SquadClientState() {}
    public static void apply(SquadStateS2CPacket packet) { snapshot = packet; }
    public static List<SquadMemberStatus> members() {
        var data = FPSMClient.getGlobalData();
        return data.isCurrentGameType(BattlezoneMap.GAME_TYPE) && data.isCurrentMap(snapshot.map())
                && data.isCurrentTeam(snapshot.team()) ? snapshot.members() : List.of();
    }
    public static String scope() { return snapshot.map() + "\0" + snapshot.team(); }
    public static void clear() { snapshot = SquadStateS2CPacket.EMPTY; }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { clear(); }
    @SubscribeEvent public static void reset(com.ptcrys.fpsmatch.common.client.event.FPSMClientResetEvent event) { clear(); }
}
