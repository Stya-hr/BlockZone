package dev.stya.blockzone.client.compat;

import com.ptcrys.fpsmatch.common.client.FPSMGameHudManager;
import net.minecraftforge.fml.ModList;

/** Match-only render overrides; Xaero's saved preferences are never changed. */
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = "blockzone", value = net.minecraftforge.api.distmarker.Dist.CLIENT)
public final class XaeroMinimapCompat {
    public static final float LEFT = 40, BOTTOM = 32, GAP = 8;
    public static final float SIZE = 320;
    private static float width = SIZE;
    private XaeroMinimapCompat() {}
    public static boolean active() {
        return ModList.get().isLoaded("xaerominimap") && FPSMGameHudManager.enable
                && com.ptcrys.fpsmatch.common.client.FPSMClient.getGlobalData()
                        .isCurrentGameType(dev.stya.blockzone.map.battlezone.BattlezoneMap.GAME_TYPE);
    }
    /** Start the minimap frame with the other HUDs, independently of terrain readiness. */
    @net.minecraftforge.eventbus.api.SubscribeEvent(priority = net.minecraftforge.eventbus.api.EventPriority.HIGH)
    public static void renderBase(net.minecraftforge.client.event.RenderGuiEvent.Pre event) {
        if (!active() || !dev.stya.blockzone.client.ui.ModernUiLayer.hudVisible()) return;
        var gui = event.getGuiGraphics();
        float scale = dev.stya.blockzone.client.hud.common.HudCanvas.scale();
        int x = Math.round(LEFT * scale);
        int bottom = gui.guiHeight() - Math.round(BOTTOM * scale);
        int size = Math.round(SIZE * scale);
        gui.fill(x, bottom - size, x + size, bottom,
                dev.stya.blockzone.client.hud.common.HudCanvas.PANEL_BACKGROUND);
        dev.stya.blockzone.client.hud.notice.NoticeHud.renderMinimapZone(event.getPartialTick());
    }
    public static boolean visiblePlayer(java.util.UUID id) {
        return visiblePlayer(id, dev.stya.blockzone.client.hud.squad.SquadClientState.members());
    }
    static boolean visiblePlayer(java.util.UUID id, java.util.List<dev.stya.blockzone.squad.SquadMemberStatus> members) {
        return members.stream()
                .anyMatch(member -> member.id().equals(id) && member.online() && member.living());
    }
    public static boolean withinMap(double x, double z) {
        var mc = net.minecraft.client.Minecraft.getInstance();
        var state = dev.stya.blockzone.client.battlezone.ZoneClientState.minimap(mc.getFrameTime());
        return state == null || dev.stya.blockzone.zone.BoundaryGeometry.of(state.x1(), state.z1(), state.x2(), state.z2())
                .containsHorizontal(x, z);
    }
    /** Never zoom out beyond 256 blocks or magnify more than 2x. */
    static double viewSpan(double width, double depth) {
        return Math.max(128, Math.min(256, Math.min(width, depth)));
    }
    public static double viewSpan() {
        var mc = net.minecraft.client.Minecraft.getInstance();
        var state = dev.stya.blockzone.client.battlezone.ZoneClientState.minimap(mc.getFrameTime());
        return state == null ? 256 : viewSpan(Math.abs((double) state.x2() - state.x1()) + 1,
                Math.abs((double) state.z2() - state.z1()) + 1);
    }
    public static double zoom() { return 256 / viewSpan(); }
    static double viewCenter(double position, double min, double max, double span) {
        return max - min <= span ? (min + max) / 2 : Math.max(min + span / 2, Math.min(max - span / 2, position));
    }
    public static double viewCenter(double position, boolean xAxis) {
        if (!active()) return position;
        var mc = net.minecraft.client.Minecraft.getInstance();
        var state = dev.stya.blockzone.client.battlezone.ZoneClientState.minimap(mc.getFrameTime());
        if (state == null) return position;
        var bounds = dev.stya.blockzone.zone.BoundaryGeometry.of(state.x1(), state.z1(), state.x2(), state.z2());
        return viewCenter(position, xAxis ? bounds.minX() : bounds.minZ(), xAxis ? bounds.maxX() : bounds.maxZ(),
                viewSpan(bounds.maxX() - bounds.minX(), bounds.maxZ() - bounds.minZ()));
    }
    public static void setWidth(float value) { width = value; }
    public static float width() { return width; }
    public static float top(float height) { return height - BOTTOM - width; }
    public static float squadOffset() { return active() ? width + GAP : 0; }
}
