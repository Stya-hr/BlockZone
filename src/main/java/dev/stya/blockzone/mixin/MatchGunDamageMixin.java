package dev.stya.blockzone.mixin;

import com.ptcrys.fpsmatch.common.attributes.ammo.GunDamageHandler;
import com.ptcrys.fpsmatch.common.event.FPSMGunDamageEvent;
import dev.stya.blockzone.combat.MatchRegeneration;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Battlezone uses gun-pack damage and consumable plates, not FPSMatch's vest/headshot rewrite. */
@Mixin(value = GunDamageHandler.class, remap = false)
public abstract class MatchGunDamageMixin {
    @Inject(method = "onEntityHurtByGun", at = @At("HEAD"), cancellable = true, remap = false)
    private static void blockzone$keepGunPackDamage(FPSMGunDamageEvent event, CallbackInfo ci) {
        if (event.getHurtEntity() instanceof ServerPlayer player && !MatchRegeneration.allowed(player)) {
            ci.cancel();
        }
    }
}
