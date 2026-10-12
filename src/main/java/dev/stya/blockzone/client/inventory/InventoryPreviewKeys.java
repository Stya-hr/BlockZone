package dev.stya.blockzone.client.inventory;

import dev.stya.blockzone.BlockZone;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = BlockZone.MOD_ID, value = Dist.CLIENT)
public final class InventoryPreviewKeys {
    public static final KeyMapping OPEN = new KeyMapping("key.blockzone.inventory_preview",
            GLFW.GLFW_KEY_I, "key.categories.blockzone");

    private InventoryPreviewKeys() { }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        while (OPEN.consumeClick()) {
            if (mc.player != null && mc.screen == null) mc.setScreen(new InventoryPanelScreen());
        }
    }

    @SubscribeEvent
    public static void hideHud(RenderGuiOverlayEvent.Pre event) {
        if (Minecraft.getInstance().screen instanceof InventoryPanelScreen) event.setCanceled(true);
    }

    @Mod.EventBusSubscriber(modid = BlockZone.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        private Registration() { }
        @SubscribeEvent
        public static void register(RegisterKeyMappingsEvent event) { event.register(OPEN); }
    }
}
