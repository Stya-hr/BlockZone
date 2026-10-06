package dev.stya.blockzone.client.battlezone;

import dev.stya.blockzone.BlockZone;
import dev.stya.blockzone.combat.RescueTimer;
import dev.stya.blockzone.net.battlezone.RescueStateS2CPacket;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BlockZone.MOD_ID, value = Dist.CLIENT)
public final class RescueClientState {
    private static final Map<UUID, RescueStateS2CPacket> STATES = new HashMap<>();
    private static final Map<net.minecraft.world.entity.player.Player, net.minecraft.world.entity.Pose> POSES = new WeakHashMap<>();
    private RescueClientState() { }
    private static void pose(net.minecraft.world.entity.player.Player player, int state) {
        if (state == 0) {
            if (POSES.containsKey(player)) player.setForcedPose(POSES.remove(player));
        } else {
            if (!POSES.containsKey(player)) POSES.put(player, player.getForcedPose());
            var forced = state == 1 ? net.minecraft.world.entity.Pose.SWIMMING : net.minecraft.world.entity.Pose.CROUCHING;
            player.setForcedPose(forced);
            player.setPose(forced);
        }
    }
    @SubscribeEvent public static void tick(net.minecraftforge.event.TickEvent.ClientTickEvent e) {
        if (e.phase != net.minecraftforge.event.TickEvent.Phase.START) return;
        var mc = Minecraft.getInstance();
        if (mc.level == null) return;
        for (var player : mc.level.players()) pose(player, state(player));
        if (mc.player != null && state(mc.player) == 1) {
            mc.options.keyJump.setDown(false);
            mc.options.keySprint.setDown(false);
            mc.options.keyAttack.setDown(false);
            mc.options.keyUse.setDown(false);
            mc.player.setSprinting(false);
        }
    }
    @SubscribeEvent public static void hand(net.minecraftforge.client.event.RenderHandEvent e) {
        var player = Minecraft.getInstance().player;
        if (player != null && state(player) != 0) e.setCanceled(true);
    }
    public static void apply(RescueStateS2CPacket p) {
        if (p.state() == 0) STATES.remove(p.playerId()); else STATES.put(p.playerId(), p);
        var level = Minecraft.getInstance().level;
        if (level != null) {
            var player = level.getPlayerByUUID(p.playerId());
            if (player != null) pose(player, p.state());
        }
    }
    public static int state(LivingEntity entity) {
        var p = STATES.get(entity.getUUID()); return p == null ? 0 : p.state();
    }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut e) {
        for (var player : List.copyOf(POSES.keySet())) pose(player, 0);
        STATES.clear(); POSES.clear();
    }
    @SubscribeEvent public static void render(RenderGuiEvent.Post e) {
        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;
        var p = STATES.get(mc.player.getUUID());
        Component label;
        if (p == null) {
            if (!(mc.hitResult instanceof net.minecraft.world.phys.EntityHitResult hit)
                    || !(hit.getEntity() instanceof net.minecraft.world.entity.player.Player target)
                    || state(target) != 1 || !mc.player.isAlliedTo(target)
                    || mc.player.distanceToSqr(target) > 6.25) return;
            label = Component.translatable("hud.blockzone.rescue_hint");
        } else label = p.state() == 1
                ? Component.translatable("hud.blockzone.downed", (p.bleedTicks() + 19) / 20)
                : Component.translatable("hud.blockzone.rescuing");
        var g = e.getGuiGraphics();
        int x = g.guiWidth() / 2 - 110, y = g.guiHeight() / 2 + 40;
        g.fill(x, y, x + 220, y + 30, 0xCC18202A);
        g.drawCenteredString(mc.font, label, x + 110, y + 7, p != null && p.state() == 1 ? 0xFFFF6868 : 0xFFE7EEF4);
        if (p != null) {
            g.fill(x + 8, y + 22, x + 212, y + 25, 0xFF394450);
            float progress = p.rescueTicks() > 0 ? p.rescueTicks() / (float)RescueTimer.RESCUE_TICKS
                    : p.bleedTicks() / (float)RescueTimer.BLEED_TICKS;
            g.fill(x + 8, y + 22, x + 8 + (int)(204 * progress), y + 25,
                    p.rescueTicks() > 0 ? 0xFF65D6A4 : 0xFFFF6868);
        }
    }
}
