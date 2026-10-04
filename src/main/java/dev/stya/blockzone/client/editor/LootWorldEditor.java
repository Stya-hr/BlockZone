package dev.stya.blockzone.client.editor;

import dev.stya.blockzone.BlockZone;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BlockZone.MOD_ID, value = Dist.CLIENT)
public final class LootWorldEditor {
    static LootCrateEditorScreen screen;
    private LootWorldEditor() {}
    static boolean start(LootCrateEditorScreen editor) {
        stop(); screen = editor;
        return LootEditorCamera.start();
    }
    static void stop() { screen = null; LootEditorCamera.stop(); }
    public static boolean hasFreeCamera() { return screen != null && LootEditorCamera.active(); }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || screen == null) return;
        var mc = Minecraft.getInstance();
        if (mc.level == null || !mc.level.dimension().location().equals(screen.packet().dimension())) stop();
        else if (mc.screen != screen && !(mc.screen instanceof LootTablePickerScreen)
                && !(mc.screen instanceof net.minecraft.client.gui.screens.ConfirmScreen)) stop();
        else LootEditorCamera.tick();
    }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { stop(); }
    @SubscribeEvent public static void fog(ViewportEvent.RenderFog event) {
        if (!hasFreeCamera() || event.getType() != net.minecraft.world.level.material.FogType.NONE) return;
        event.setNearPlaneDistance(4096); event.setFarPlaneDistance(8192); event.setCanceled(true);
    }
    @SubscribeEvent public static void render(RenderLevelStageEvent event) {
        if (screen == null || event.getStage() != RenderLevelStageEvent.Stage.AFTER_SOLID_BLOCKS) return;
        var mc = Minecraft.getInstance();
        if (mc.level == null || !mc.level.dimension().location().equals(screen.packet().dimension())) return;
        var camera = event.getCamera().getPosition();
        var buffers = mc.renderBuffers().bufferSource();
        var lines = buffers.getBuffer(RenderType.lines());
        var stack = event.getPoseStack();
        for (var crate : screen.draft().visible()) {
            boolean selected = screen.draft().selected(crate);
            LevelRenderer.renderLineBox(stack, lines, new AABB(crate.x() - .02, crate.y() - .02, crate.z() - .02,
                    crate.x() + 1.02, crate.y() + 1.45, crate.z() + 1.02).move(-camera.x, -camera.y, -camera.z),
                    selected ? .3f : 1f, selected ? 1f : .7f, selected ? .55f : .25f, 1f);
        }
        buffers.endBatch(RenderType.lines());
    }
}
