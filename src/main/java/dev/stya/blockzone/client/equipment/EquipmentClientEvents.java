package dev.stya.blockzone.client.equipment;

import dev.stya.blockzone.BlockZone;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BlockZone.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class EquipmentClientEvents {
    @SubscribeEvent public static void reload(ModelEvent.BakingCompleted event) { TacticalArmorExtension.clearCache(); }
}
