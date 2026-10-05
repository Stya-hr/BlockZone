package dev.stya.blockzone.loot;

import dev.stya.blockzone.BlockZone;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ChunkWatchEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=BlockZone.MOD_ID)
public final class LootCrateEvents {
    private LootCrateEvents() { }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void interact(PlayerInteractEvent.RightClickBlock event) {
        if(event.getLevel() instanceof ServerLevel level && event.getEntity() instanceof ServerPlayer player
                && LootContainerControl.interact(level.getBlockEntity(event.getPos()),player)) {
            event.setCancellationResult(InteractionResult.CONSUME); event.setCanceled(true);
        }
    }
    @SubscribeEvent public static void breakCrate(BlockEvent.BreakEvent event) {
        if(event.getLevel() instanceof ServerLevel level && LootContainerControl.isControlled(level.getBlockEntity(event.getPos()))
                && !LootCrateAccess.mayEdit(level,event.getPos())) event.setCanceled(true);
    }
    @SubscribeEvent public static void placeCrate(BlockEvent.EntityPlaceEvent event) {
        if(event.getLevel() instanceof ServerLevel level && LootContainerControl.isControlled(level.getBlockEntity(event.getPos()))
                && !LootCrateAccess.mayEdit(level,event.getPos())) event.setCanceled(true);
    }
    @SubscribeEvent public static void watch(ChunkWatchEvent.Watch event) {
        var level=event.getPlayer().serverLevel(); var chunk=level.getChunk(event.getPos().x,event.getPos().z);
        for(var container:chunk.getBlockEntities().values()) {
            if(LootContainerControl.isControlled(container)) LootContainerAdapters.find(container).setOpened(container,LootContainerControl.data(container).opened());
        }
    }
}
