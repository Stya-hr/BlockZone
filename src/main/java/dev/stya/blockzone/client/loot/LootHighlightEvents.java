package dev.stya.blockzone.client.loot;

import dev.stya.blockzone.BlockZone;
import dev.stya.blockzone.loot.LootDropEntity;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BlockZone.MOD_ID, value = Dist.CLIENT)
public final class LootHighlightEvents {
    private static final double RANGE_SQUARED = 6 * 6;
    private LootHighlightEvents() { }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var mc = Minecraft.getInstance();
        if (mc.level == null) return;
        for (var entity : mc.level.entitiesForRendering()) {
            if (entity instanceof LootDropEntity drop) {
                // Client-only: another player's proximity cannot reveal distant loot to this player.
                drop.setClientHighlighted(mc.player != null && mc.player.distanceToSqr(drop) <= RANGE_SQUARED);
            }
        }
    }
}
