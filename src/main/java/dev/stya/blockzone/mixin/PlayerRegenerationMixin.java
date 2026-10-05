package dev.stya.blockzone.mixin;

import dev.stya.blockzone.combat.MatchRegeneration;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Player.class)
public abstract class PlayerRegenerationMixin {
    // Peaceful difficulty heals independently of FoodData's saturation-based regeneration.
    @Redirect(method = "aiStep", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/GameRules;getBoolean(Lnet/minecraft/world/level/GameRules$Key;)Z"))
    private boolean blockzone$peacefulRegeneration(GameRules rules, GameRules.Key<GameRules.BooleanValue> key) {
        return rules.getBoolean(key) && MatchRegeneration.allowed((Player) (Object) this);
    }
}
