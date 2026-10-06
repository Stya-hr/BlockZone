package dev.stya.blockzone.combat;

import dev.stya.blockzone.BlockZone;
import dev.stya.blockzone.net.battlezone.BattlezoneNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.player.*;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BlockZone.MOD_ID)
public final class DownedEvents {
    private DownedEvents() { }
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void damage(LivingDamageEvent e) {
        if (e.getEntity() instanceof ServerPlayer p && e.getAmount() > 0) {
            if (DownedController.duplicateBulletDamage(p, e.getSource())) { e.setCanceled(true); return; }
            DownedController.interrupted(p);
            if (e.getAmount() >= p.getHealth() && DownedController.knock(p, e.getSource())) e.setCanceled(true);
        }
    }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent e) {
        if (e.phase == TickEvent.Phase.END) DownedController.tick();
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) DownedController.clear(p);
    }
    @SubscribeEvent public static void tracking(PlayerEvent.StartTracking e) {
        if (e.getEntity() instanceof ServerPlayer p && e.getTarget() instanceof ServerPlayer target)
            BattlezoneNetwork.sendEditor(p, BattlezoneNetwork.rescuePacket(target));
    }
    @SubscribeEvent public static void attack(AttackEntityEvent e) {
        if (e.getEntity() instanceof ServerPlayer p && DownedController.busy(p)) e.setCanceled(true);
    }
    @SubscribeEvent public static void interact(PlayerInteractEvent e) {
        if (e.isCancelable() && e.getEntity() instanceof ServerPlayer p && DownedController.busy(p)) e.setCanceled(true);
    }
    @SubscribeEvent public static void use(net.minecraftforge.event.entity.living.LivingEntityUseItemEvent.Start e) {
        if (e.getEntity() instanceof ServerPlayer p && DownedController.busy(p)) e.setCanceled(true);
    }
    @SubscribeEvent public static void heal(net.minecraftforge.event.entity.living.LivingHealEvent e) {
        if (e.getEntity() instanceof ServerPlayer p && DownedController.down(p)) e.setCanceled(true);
    }
    @SubscribeEvent public static void jump(net.minecraftforge.event.entity.living.LivingEvent.LivingJumpEvent e) {
        if (e.getEntity() instanceof ServerPlayer p && DownedController.down(p)) {
            var v = p.getDeltaMovement(); p.setDeltaMovement(v.x, Math.min(0, v.y), v.z);
        }
    }
}
