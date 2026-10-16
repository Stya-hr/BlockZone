package dev.stya.blockzone.chat;

import com.ptcrys.fpsmatch.core.FPSMCore;
import dev.stya.blockzone.BlockZone;
import dev.stya.blockzone.map.battlezone.BattlezoneMap;
import dev.stya.blockzone.net.BlockzoneNetwork;
import dev.stya.blockzone.net.battlezone.MatchChatS2CPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

@Mod.EventBusSubscriber(modid = BlockZone.MOD_ID)
public final class MatchChat {
    private static final Map<ServerPlayer, Long> lastMessage = new WeakHashMap<>();
    private MatchChat() {}
    private static java.util.Optional<BattlezoneMap> map(ServerPlayer player) {
        return FPSMCore.getInstance().getMapByPlayerWithSpec(player)
                .filter(BattlezoneMap.class::isInstance).map(BattlezoneMap.class::cast)
                .filter(m -> m.getServerLevel() == player.serverLevel());
    }
    public static String normalize(String text) {
        if (text.length() > 256 || text.chars().anyMatch(c -> !net.minecraft.SharedConstants.isAllowedChatCharacter((char)c))) return "";
        String value = text.trim();
        return value.startsWith("/") ? "" : value;
    }
    public static boolean canReceive(BattlezoneMap map, UUID sender, UUID recipient, ChatChannel channel) {
        var from = map.getMapTeams().getTeamByPlayer(sender);
        var to = map.getMapTeams().getTeamByPlayer(recipient);
        return from.isPresent() && to.isPresent() && (channel == ChatChannel.ALL
                || !from.get().isSpectator() && from.get() == to.get());
    }
    /** The server resolves membership; packets never contain a recipient list or team name. */
    public static void send(ServerPlayer player, ChatChannel channel, String text) {
        if (player.getChatVisibility() != net.minecraft.world.entity.player.ChatVisiblity.FULL
                || map(player).isEmpty() || normalize(text).isEmpty()) return;
        var event = new ScopedEvent(player, text, Component.literal(normalize(text)));
        if (!MinecraftForge.EVENT_BUS.post(event)) dispatch(player, channel, event.getMessage().getString());
    }
    private static void dispatch(ServerPlayer player, ChatChannel channel, String text) {
        String body = normalize(text);
        if (body.isEmpty()) return;
        long now = net.minecraft.Util.getMillis();
        if (now - lastMessage.getOrDefault(player, Long.MIN_VALUE / 2) < 500) return;
        map(player).ifPresent(map -> {
            if (!canReceive(map, player.getUUID(), player.getUUID(), channel)) return;
            lastMessage.put(player, now);
            var packet = new MatchChatS2CPacket(map.getMapName(), channel, player.getUUID(), player.getGameProfile().getName(), body);
            for (var recipient : map.getMapTeams().getOnlineWithSpec())
                if (recipient.serverLevel() == map.getServerLevel() && canReceive(map, player.getUUID(), recipient.getUUID(), channel))
                    BlockzoneNetwork.sendToPlayer(recipient, packet);
            org.apache.logging.log4j.LogManager.getLogger().info("[{}][{}] <{}> {}", map.getMapName(), channel, player.getGameProfile().getName(), body);
        });
    }
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void vanilla(ServerChatEvent event) {
        if (event instanceof ScopedEvent || map(event.getPlayer()).isEmpty()) return;
        event.setCanceled(true);
        dispatch(event.getPlayer(), ChatChannel.ALL, event.getMessage().getString());
    }
    private static final class ScopedEvent extends ServerChatEvent {
        private ScopedEvent(ServerPlayer player, String raw, Component message) { super(player, raw, message); }
    }
}
