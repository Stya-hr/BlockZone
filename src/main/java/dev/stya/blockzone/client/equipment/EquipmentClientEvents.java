package dev.stya.blockzone.client.equipment;

import dev.stya.blockzone.BlockZone;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = BlockZone.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class EquipmentClientEvents {
    public static final KeyMapping DETACH = new KeyMapping("key.blockzone.detach_armor", GLFW.GLFW_KEY_N, "key.categories.blockzone");
    @SubscribeEvent public static void keys(RegisterKeyMappingsEvent event) { event.register(DETACH); }
    @SubscribeEvent public static void reload(ModelEvent.BakingCompleted event) { TacticalArmorExtension.clearCache(); }
}
