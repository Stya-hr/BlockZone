package dev.stya.blockzone.mixin;

import dev.stya.blockzone.combat.MatchRegeneration;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.level.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(FoodData.class)
public abstract class FoodDataMixin {
    @Redirect(method = "tick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/GameRules;getBoolean(Lnet/minecraft/world/level/GameRules$Key;)Z"))
    private boolean blockzone$naturalRegeneration(GameRules rules, GameRules.Key<GameRules.BooleanValue> key,
                                                 Player player) {
        return rules.getBoolean(key) && MatchRegeneration.allowed(player);
    }
}
