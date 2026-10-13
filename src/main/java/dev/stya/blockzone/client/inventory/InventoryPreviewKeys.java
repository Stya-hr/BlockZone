package dev.stya.blockzone.client.inventory;

import dev.stya.blockzone.BlockZone;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = BlockZone.MOD_ID, value = Dist.CLIENT)
public final class InventoryPreviewKeys {
    public static final KeyMapping OPEN = new KeyMapping("key.blockzone.inventory_preview",
            GLFW.GLFW_KEY_TAB, "key.categories.blockzone");

    public static java.util.List<KeyMapping> movementKeys(Minecraft mc) {
        return java.util.List.of(mc.options.keyUp, mc.options.keyDown, mc.options.keyLeft, mc.options.keyRight,
                mc.options.keyJump, mc.options.keyShift, mc.options.keySprint).stream()
                .filter(key -> key != mc.options.keyShift || (key.getKey().getValue() != GLFW.GLFW_KEY_LEFT_SHIFT
                        && key.getKey().getValue() != GLFW.GLFW_KEY_RIGHT_SHIFT)).toList();
    }

    private InventoryPreviewKeys() { }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        while (OPEN.consumeClick()) {
            if (mc.player != null && mc.screen == null && movementKeys(mc).stream().noneMatch(KeyMapping::isDown)) {
                boolean crouching = mc.options.keyShift.isDown();
                mc.setScreen(new InventoryPanelScreen());
                mc.options.keyShift.setDown(crouching);
            }
        }
    }

    @SubscribeEvent
    public static void hideHud(RenderGuiEvent.Pre event) {
        if (Minecraft.getInstance().screen instanceof InventoryPanelScreen) event.setCanceled(true);
    }

    @Mod.EventBusSubscriber(modid = BlockZone.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        private Registration() { }
        @SubscribeEvent
        public static void register(RegisterKeyMappingsEvent event) { event.register(OPEN); }
        @SubscribeEvent
        public static void reload(RegisterClientReloadListenersEvent event) {
            event.registerReloadListener((net.minecraft.server.packs.resources.ResourceManagerReloadListener)
                    manager -> InventorySvgAssets.clear());
        }
    }
}
