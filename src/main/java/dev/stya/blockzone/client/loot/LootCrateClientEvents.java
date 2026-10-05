package dev.stya.blockzone.client.loot;

import dev.stya.blockzone.BlockZone;
import dev.stya.blockzone.registry.BlockzoneEntities;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BlockZone.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class LootCrateClientEvents {
    private LootCrateClientEvents() { }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(BlockzoneEntities.LOOT_DROP.get(), LootDropRenderer::new);
    }
}
