package dev.stya.blockzone.client.hud.chat;

import com.ptcrys.fpsmatch.common.client.FPSMClient;
import com.ptcrys.fpsmatch.common.client.FPSMGameHudManager;
import dev.stya.blockzone.BlockZone;
import dev.stya.blockzone.client.ui.ModernUiLayer;
import dev.stya.blockzone.map.battlezone.BattlezoneMap;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.ChatVisiblity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = BlockZone.MOD_ID, value = Dist.CLIENT)
public final class ChatHud {
    private static final List<ChatMessage> messages = new ArrayList<>();
    private static final ChatMessagePanel panel = new ChatMessagePanel();
    private ChatHud() {}
    public static boolean active() {
        return FPSMGameHudManager.shouldRender()
                && FPSMClient.getGlobalData().isCurrentGameType(BattlezoneMap.GAME_TYPE);
    }
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void received(ClientChatReceivedEvent event) {
        if (event instanceof ClientChatReceivedEvent.System system && system.isOverlay()) return;
        if (active() && event instanceof ClientChatReceivedEvent.Player) return;
        String name = "", body = event.getMessage().getString();
        if (event instanceof ClientChatReceivedEvent.Player player) {
            var original = player.getPlayerChatMessage();
            var filtered = original.filterMask().isEmpty() ? original.decoratedContent()
                    : original.filterMask().applyWithFormatting(original.signedContent());
            // Keep other mods' rewritten messages intact rather than stripping arbitrary prefixes.
            if (filtered != null && event.getBoundChatType().decorate(filtered).getString().equals(body)) {
                name = event.getBoundChatType().name().getString();
                body = filtered.getString();
            }
        }
        if (body.isBlank()) return;
        messages.add(new ChatMessage(event.getSender(), name, body, net.minecraft.Util.getMillis()));
        if (messages.size() > 100) messages.remove(0);
        // Do not cancel: vanilla still owns chat history, signatures, reporting and interactive links.
    }
    public static void receive(dev.stya.blockzone.net.battlezone.MatchChatS2CPacket packet) {
        var mc = Minecraft.getInstance();
        var data = FPSMClient.getGlobalData();
        if (mc.player == null || !data.isCurrentGameType(BattlezoneMap.GAME_TYPE) || !data.isCurrentMap(packet.map())
                || mc.isBlocked(packet.sender()) || mc.options.chatVisibility().get() != ChatVisiblity.FULL) return;
        messages.add(new ChatMessage(packet.sender(), packet.name(), packet.text(), net.minecraft.Util.getMillis(), packet.channel()));
        if (messages.size() > 100) messages.remove(0);
        mc.gui.getChat().addMessage(net.minecraft.network.chat.Component.literal(packet.name() + ": " + packet.text()));
    }
    static List<ChatMessage> messages() {
        return Minecraft.getInstance().options.chatVisibility().get() == ChatVisiblity.SYSTEM
                ? messages.stream().filter(m -> m.name().isEmpty()).toList() : List.copyOf(messages);
    }
    @SubscribeEvent
    public static void open(net.minecraftforge.client.event.ScreenEvent.Opening event) {
        if (active() && event.getNewScreen() instanceof net.minecraft.client.gui.screens.ChatScreen chat)
            event.setNewScreen(new ModernChatScreen(((dev.stya.blockzone.mixin.client.ChatScreenInitialAccessor)chat).blockzone$initial()));
    }
    @SubscribeEvent
    public static void render(RenderGuiOverlayEvent.Pre event) {
        var mc = Minecraft.getInstance();
        if (event.getOverlay() != VanillaGuiOverlay.CHAT_PANEL.type() || !active()
                || !ModernUiLayer.hudVisible()) return;
        event.setCanceled(true);
        if (mc.screen instanceof ModernChatScreen) return;
        if (mc.options.chatVisibility().get() == ChatVisiblity.HIDDEN) return;
        var visible = mc.options.chatVisibility().get() == ChatVisiblity.SYSTEM
                ? messages.stream().filter(m -> m.name().isEmpty()).toList() : List.copyOf(messages);
        panel.render(ModernUiLayer.hud(), visible, mc.player.getUUID(), net.minecraft.Util.getMillis(),
                (float)(mc.options.chatOpacity().get() * .9 + .1));
    }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { messages.clear(); }
    @SubscribeEvent public static void reset(com.ptcrys.fpsmatch.common.client.event.FPSMClientResetEvent event) { messages.clear(); }
}
