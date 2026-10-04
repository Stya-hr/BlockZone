package dev.stya.blockzone.loot;

import dev.stya.blockzone.BlockZone;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BlockZone.MOD_ID)
public final class LootCrateEvents {
    private LootCrateEvents() { }

    @SubscribeEvent
    public static void breakCrate(BlockEvent.BreakEvent event) {
        if (event.getState().getBlock() instanceof LootCrateBlock && event.getLevel() instanceof ServerLevel level
                && !LootCrateAccess.mayEdit(level, event.getPos())) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void placeCrate(BlockEvent.EntityPlaceEvent event) {
        if (event.getPlacedBlock().getBlock() instanceof LootCrateBlock && event.getLevel() instanceof ServerLevel level
                && !LootCrateAccess.mayEdit(level, event.getPos())) event.setCanceled(true);
    }
}
