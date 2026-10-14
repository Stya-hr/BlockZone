package dev.stya.blockzone.combat;

import com.ptcrys.fpsmatch.core.damage.DamageSourceCategory;
import com.ptcrys.fpsmatch.core.damage.MinecraftDamageSourceClassifier;
import dev.stya.blockzone.BlockZone;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;

/** Only mode-specific types live here; FPSMatch owns classification. */
public final class CombatDamageTypes {
    public static final ResourceKey<DamageType> POISON_ZONE = ResourceKey.create(Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(BlockZone.MOD_ID, "poison_zone"));
    private CombatDamageTypes() { }

    public static void register() {
        MinecraftDamageSourceClassifier.registerType(POISON_ZONE, DamageSourceCategory.ENVIRONMENT);
    }

    public static DamageSource poisonZone(ServerLevel level) {
        return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(POISON_ZONE));
    }
}
