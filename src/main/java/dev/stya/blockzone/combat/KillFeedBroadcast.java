package dev.stya.blockzone.combat;

import dev.stya.blockzone.BlockZone;
import dev.stya.blockzone.map.battlezone.BattlezoneMap;
import dev.stya.blockzone.net.BlockzoneNetwork;
import dev.stya.blockzone.net.battlezone.KillFeedS2CPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerPlayer;
import com.ptcrys.fpsmatch.core.map.DeathContext;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Sends successful transitions with server-resolved actors and original damage weapons. */
@Mod.EventBusSubscriber(modid = BlockZone.MOD_ID)
public final class KillFeedBroadcast {
    private KillFeedBroadcast() { }
    @SubscribeEvent public static void eliminated(BattlezoneDeathEvent event) {
        var map = (BattlezoneMap)event.getMap();
        if (!map.isMatchActive()) return;
        var context = event.getContext();
        damaged(map, CombatFeedKind.ELIMINATED, context);
    }
    public static void downed(BattlezoneMap map, DeathContext context) {
        damaged(map, CombatFeedKind.DOWNED, context);
    }
    private static void damaged(BattlezoneMap map, CombatFeedKind kind, DeathContext context) {
        send(map, kind, context.getAttacker(), context.getDeadPlayer(),
                context.getAttacker() == null ? ItemStack.EMPTY : context.getDeathItem().copyWithCount(1));
    }
    @SubscribeEvent public static void revived(BattlezoneReviveEvent event) {
        send((BattlezoneMap)event.getMap(), CombatFeedKind.REVIVED, event.getSource(), event.getPlayer(), ItemStack.EMPTY);
    }
    private static void send(BattlezoneMap map, CombatFeedKind kind, ServerPlayer actor, ServerPlayer target, ItemStack weapon) {
        if (!map.isMatchActive()) return;
        var packet = new KillFeedS2CPacket(map.getMapName(), kind, actor == null ? null : actor.getUUID(),
                actor == null ? "" : actor.getGameProfile().getName(), target.getUUID(), target.getGameProfile().getName(), weapon);
        for (var recipient : map.getMapTeams().getOnlineWithSpec())
            if (recipient.serverLevel() == map.getServerLevel()) BlockzoneNetwork.sendToPlayer(recipient, packet);
    }
}
