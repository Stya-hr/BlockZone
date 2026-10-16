package dev.stya.blockzone.client.inventory;

import dev.stya.blockzone.BlockZone;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;

import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = BlockZone.MOD_ID, value = Dist.CLIENT)
public final class InventoryPreviewKeys {
    public static java.util.List<KeyMapping> movementKeys(Minecraft mc) {
        return java.util.List.of(mc.options.keyUp, mc.options.keyDown, mc.options.keyLeft, mc.options.keyRight,
                mc.options.keyJump, mc.options.keyShift, mc.options.keySprint).stream()
                .filter(key -> key != mc.options.keyShift).toList();
    }

    public static boolean bulkDropDown(Minecraft mc) {
        var key = mc.options.keyShift.getKey();
        long window = mc.getWindow().getWindow();
        if (key.getType() == com.mojang.blaze3d.platform.InputConstants.Type.MOUSE)
            return GLFW.glfwGetMouseButton(window, key.getValue()) == GLFW.GLFW_PRESS;
        if (key.getValue() == GLFW.GLFW_KEY_LEFT_SHIFT || key.getValue() == GLFW.GLFW_KEY_RIGHT_SHIFT)
            return com.mojang.blaze3d.platform.InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_SHIFT)
                    || com.mojang.blaze3d.platform.InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_SHIFT);
        return key.getValue() >= 0 && com.mojang.blaze3d.platform.InputConstants.isKeyDown(window, key.getValue());
    }

    private InventoryPreviewKeys() { }

    @SubscribeEvent
    public static void openInventory(ScreenEvent.Opening event) {
        if (dev.stya.blockzone.client.battlezone.DeploymentClientController.state() == 1
                && (event.getNewScreen() instanceof InventoryPanelScreen
                    || event.getNewScreen() instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?>)) {
            event.setCanceled(true);
            return;
        }
        if (!(event.getNewScreen() instanceof InventoryScreen)
                && !(event.getNewScreen() instanceof CreativeModeInventoryScreen)) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (movementKeys(mc).stream().anyMatch(KeyMapping::isDown)) {
            event.setCanceled(true);
            return;
        }
        event.setNewScreen(new InventoryPanelScreen());
    }

    @Mod.EventBusSubscriber(modid = BlockZone.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        private Registration() { }
        @SubscribeEvent
        public static void reload(RegisterClientReloadListenersEvent event) {
            event.registerReloadListener((net.minecraft.server.packs.resources.ResourceManagerReloadListener)
                    manager -> {
                        InventorySvgAssets.clear();
                        GunHudIcon.clear();
                        dev.stya.blockzone.client.ui.ModernUiLayer.clearAssets();
                    });
        }
    }
}
