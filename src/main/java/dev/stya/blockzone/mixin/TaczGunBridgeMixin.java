package dev.stya.blockzone.mixin;

import com.ptcrys.fpsmatch.common.event.FPSMGunDamageEvent;
import com.tacz.guns.api.event.common.EntityHurtByGunEvent;
import dev.stya.blockzone.map.battlezone.MatchRegeneration;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.IEventBus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Seed the framework event with TaCZ's multiplier before its listeners run. */
@Pseudo
@Mixin(targets = "com.ptcrys.fpsmatch.compat.tacz.TACZGunEventBridge", remap = false)
public abstract class TaczGunBridgeMixin {
    @Redirect(method = "onEntityHurtByGun", at = @At(value = "INVOKE",
            target = "Lnet/minecraftforge/eventbus/api/IEventBus;post(Lnet/minecraftforge/eventbus/api/Event;)Z"), remap = false)
    private static boolean blockzone$preserveHeadshotMultiplier(IEventBus bus, Event event,
                                                               EntityHurtByGunEvent.Pre original) {
        if (event instanceof FPSMGunDamageEvent damage
                && damage.getHurtEntity() instanceof ServerPlayer player
                && !MatchRegeneration.allowed(player)) {
            damage.setHeadshotMultiplier(original.getHeadshotMultiplier());
        }
        return bus.post(event);
    }
}
