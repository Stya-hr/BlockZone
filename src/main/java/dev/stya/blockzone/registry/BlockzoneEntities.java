package dev.stya.blockzone.registry;

import dev.stya.blockzone.BlockZone;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import dev.stya.blockzone.loot.LootDropEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public final class BlockzoneEntities {
    private static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, BlockZone.MOD_ID);

    public static final RegistryObject<EntityType<LootDropEntity>> LOOT_DROP = ENTITIES.register("loot_drop",
            () -> EntityType.Builder.<LootDropEntity>of(LootDropEntity::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F).clientTrackingRange(8).updateInterval(2)
                    .build(BlockZone.MOD_ID + ":loot_drop"));

    private BlockzoneEntities() { }
    public static void register(IEventBus bus) { ENTITIES.register(bus); }
}
