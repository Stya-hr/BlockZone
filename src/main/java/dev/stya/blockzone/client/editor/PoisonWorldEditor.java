package dev.stya.blockzone.client.editor;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import dev.stya.blockzone.BlockZone;
import dev.stya.blockzone.client.battlezone.WorldRenderer;
import dev.stya.blockzone.editor.zone.PoisonEditorDraft;
import dev.stya.blockzone.net.battlezone.BattlezoneNetwork;
import dev.stya.blockzone.net.battlezone.BoundaryPreviewS2CPacket;
import dev.stya.blockzone.net.editor.*;
import dev.stya.blockzone.net.editor.PoisonEditorResultS2CPacket;
import dev.stya.blockzone.net.editor.PoisonEditorS2CPacket;
import dev.stya.blockzone.net.editor.PoisonEditorSessions;
import dev.stya.blockzone.net.editor.SavePoisonEditorC2SPacket;
import dev.stya.blockzone.zone.BoundaryGeometry;
import dev.stya.blockzone.zone.PoisonPath;
import dev.stya.blockzone.zone.ZoneGeometry;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = BlockZone.MOD_ID, value = Dist.CLIENT)
public final class PoisonWorldEditor {
    static PoisonEditorS2CPacket session;
    static PoisonEditorDraft draft;
    static String message = "";
    static boolean saving;
    static String savedJson;
    private PoisonWorldEditor() {}
    public static void open(PoisonEditorS2CPacket packet) {
        var mc = Minecraft.getInstance();
        if (mc.level == null || !mc.level.dimension().location().equals(packet.dimension())) return;
        session = packet;
        draft = new PoisonEditorDraft(PoisonPath.CODEC.listOf().parse(JsonOps.INSTANCE,
                JsonParser.parseString(packet.pathsJson())).getOrThrow(false, m -> {}));
        savedJson = PoisonEditorSessions.json(draft.paths());
        saving = false; message = "";
        if (!PoisonEditorCamera.start()) message = Component.translatable("editor.blockzone.camera_busy").getString();
        mc.setScreen(new PoisonEditorScreen());
    }
    public static boolean hasFreeCamera() { return session != null && PoisonEditorCamera.active(); }
    static BoundaryGeometry bounds() {
        return BoundaryGeometry.of(session.pos1().getX(), session.pos1().getZ(), session.pos2().getX(), session.pos2().getZ());
    }
    static double y() { return ZoneGeometry.centerY(session.pos1().getY(), session.pos2().getY()); }
    static void setGeometry(double x, double z, double radius) {
        var c = draft.selected();
        draft.replace(new PoisonPath.Circle(x, z, Math.max(0, Math.min(30000000, radius)),
                c.waitSeconds(), c.shrinkSeconds(), c.damageMultiplier()));
    }
    static void save() {
        if (saving) return;
        try {
            for (var path : draft.paths()) path.resolve(bounds(), y());
            String json = PoisonEditorSessions.json(draft.paths());
            if (json.length() > 262144) throw new IllegalArgumentException("Draft is too large for the editor");
            BattlezoneNetwork.saveEditor(new SavePoisonEditorC2SPacket(session.token(), json));
            saving = true; message = "Saving...";
        } catch (RuntimeException exception) { message = exception.getMessage(); }
    }
    public static void result(PoisonEditorResultS2CPacket packet) {
        if (session == null || !session.token().equals(packet.token())) return;
        saving = false; message = packet.message();
        if (packet.saved()) savedJson = PoisonEditorSessions.json(draft.paths());
    }
    static boolean dirty() { return draft != null && !PoisonEditorSessions.json(draft.paths()).equals(savedJson); }
    static void discard() { session = null; draft = null; saving = false; PoisonEditorCamera.stop(); Minecraft.getInstance().setScreen(null); }
    public static void render(RenderLevelStageEvent event) {
        var mc = Minecraft.getInstance();
        if (session == null || mc.level == null || !mc.level.dimension().location().equals(session.dimension())
                || event.getStage() != RenderLevelStageEvent.Stage.AFTER_SOLID_BLOCKS) return;
        var camera = event.getCamera().getPosition();
        WorldRenderer.renderPreview(new BoundaryPreviewS2CPacket(session.dimension(), true, session.pos1(), session.pos2()),
                event.getPoseStack(), camera);
        for (int i = Math.max(0, draft.circle() - 1); i <= Math.min(draft.path().circles().size() - 1, draft.circle() + 1); i++) {
            var c = draft.path().circles().get(i);
            var raw = new ZoneGeometry(c.x(), y(), c.z(), c.radius(), PoisonWorldEditor.draft.path().shape());
            int color = i == draft.circle() ? 0xffffff : net.minecraft.util.Mth.hsvToRgb(
                    (float)i / draft.path().circles().size(), .65f, .65f);
            try {
                var actual = raw.fitInside(bounds());
                boolean moved = actual.centerX() != raw.centerX() || actual.centerZ() != raw.centerZ();
                WorldRenderer.renderZoneGrid(raw, event.getPoseStack(), camera, moved ? 0xffa030 : color);
                if (moved && i == draft.circle()) WorldRenderer.renderZoneGrid(actual, event.getPoseStack(), camera, 0x30ffff);
            } catch (IllegalArgumentException invalid) {
                WorldRenderer.renderZoneGrid(raw, event.getPoseStack(), camera, 0xff3030);
            }
        }
    }
    @SubscribeEvent public static void tick(net.minecraftforge.event.TickEvent.ClientTickEvent event) {
        var mc = Minecraft.getInstance();
        if (event.phase != net.minecraftforge.event.TickEvent.Phase.END || session == null) return;
        if (mc.level == null || !mc.level.dimension().location().equals(session.dimension())) {
            session = null; draft = null; saving = false; PoisonEditorCamera.stop();
            if (mc.screen instanceof PoisonEditorScreen) mc.setScreen(null);
        } else PoisonEditorCamera.tick();
    }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { session = null; draft = null; PoisonEditorCamera.stop(); }
    @SubscribeEvent public static void key(InputEvent.Key event) {
        var mc = Minecraft.getInstance();
        if (session == null) return;
        PoisonEditorCamera.key(event.getKey(), event.getScanCode(), event.getAction());
        if (mc.screen != null || event.getAction() != GLFW.GLFW_PRESS) return;
        if (event.getKey() == GLFW.GLFW_KEY_F6) PoisonEditorCamera.focus();
        if (saving) return;
        if (event.getKey() == GLFW.GLFW_KEY_O) mc.setScreen(new PoisonEditorScreen());
        if (event.getKey() == GLFW.GLFW_KEY_LEFT_BRACKET) draft.selectCircle(draft.circle() - 1);
        if (event.getKey() == GLFW.GLFW_KEY_RIGHT_BRACKET) draft.selectCircle(draft.circle() + 1);
    }
    @SubscribeEvent(priority = net.minecraftforge.eventbus.api.EventPriority.LOWEST)
    public static void fog(ViewportEvent.RenderFog event) {
        if (session == null || !PoisonEditorCamera.active()
                || event.getType() != net.minecraft.world.level.material.FogType.NONE) return;
        // An aerial overview can be farther from the map than the normal gameplay fog distance.
        event.setNearPlaneDistance(4096);
        event.setFarPlaneDistance(8192);
        event.setCanceled(true);
    }
    @SubscribeEvent public static void hud(RenderGuiEvent.Post event) {
        var mc = Minecraft.getInstance();
        if (session == null || mc.screen != null) return;
        var graphics = event.getGuiGraphics();
        graphics.fill(6, 6, 440, 63, 0xb0000000);
        graphics.drawString(mc.font, Component.translatable("editor.blockzone.observing", draft.sequence()+1, draft.circle()+1,
                String.format(java.util.Locale.ROOT, "%.1f", draft.selected().radius())), 10, 10, 0xffffff);
        graphics.drawString(mc.font, Component.translatable("editor.blockzone.world_controls"), 10, 25, 0xffffff);
        var position = PoisonEditorCamera.position();
        if (position != null) graphics.drawString(mc.font, Component.translatable("editor.blockzone.camera_position",
                String.format(java.util.Locale.ROOT, "%.1f", position.x), String.format(java.util.Locale.ROOT, "%.1f", position.y),
                String.format(java.util.Locale.ROOT, "%.1f", position.z)), 10, 43, 0x80ffff);
    }
}
