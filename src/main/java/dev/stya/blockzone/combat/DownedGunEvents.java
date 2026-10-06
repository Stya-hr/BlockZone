package dev.stya.blockzone.combat;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class DownedGunEvents {
    public static void register() {
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(DownedGunEvents.class);
    }
    @SubscribeEvent public static void shoot(com.tacz.guns.api.event.common.GunShootEvent e) {
        if (e.getShooter() instanceof ServerPlayer p && DownedController.busy(p)) e.setCanceled(true);
    }
    @SubscribeEvent public static void fire(com.tacz.guns.api.event.common.GunFireEvent e) {
        if (e.getShooter() instanceof ServerPlayer p && DownedController.busy(p)) e.setCanceled(true);
    }
}
