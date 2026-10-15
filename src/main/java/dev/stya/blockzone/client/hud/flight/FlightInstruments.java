package dev.stya.blockzone.client.hud.flight;

import dev.stya.blockzone.client.battlezone.DeploymentClientController;
import dev.stya.blockzone.client.battlezone.DeploymentVehicleState;
import dev.stya.blockzone.client.hud.action.ActionStatus;
import dev.stya.blockzone.client.hud.common.HudCanvas;
import java.lang.ref.WeakReference;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Flight state is server-owned; ground sampling never loads a missing client chunk. */
public final class FlightInstruments {
    private final FlightGauge gauge = new FlightGauge();
    private WeakReference<net.minecraft.client.multiplayer.ClientLevel> level = new WeakReference<>(null);
    private long sampledTick = Long.MIN_VALUE;
    private double ground = Double.NaN;
    public ActionStatus prompt() {
        int state = DeploymentClientController.state();
        return new ActionStatus(tr(state == 1 ? "jump" : state == 2 ? "close" : "open"),
                tr(state == 1 ? "transport" : state == 2 ? "gliding" : "freefall"),
                state == 1 ? DeploymentClientController.releaseKey() : DeploymentClientController.parachuteKey(), null, HudCanvas.TEXT);
    }
    public void render(HudCanvas g, float partial) {
        var mc = Minecraft.getInstance();
        var player = mc.player;
        long now = mc.level.getGameTime();
        if (level.get() != mc.level || now < sampledTick || now - sampledTick >= 4 || sampledTick == Long.MIN_VALUE) {
            level = new WeakReference<>(mc.level); sampledTick = now; ground = Double.NaN;
            if (mc.level.hasChunk(player.chunkPosition().x, player.chunkPosition().z)) {
                var from = player.position();
                var hit = mc.level.clip(new ClipContext(from, new Vec3(from.x, mc.level.getMinBuildHeight(), from.z),
                        ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, player));
                if (hit.getType() == HitResult.Type.BLOCK) ground = hit.getLocation().y;
            }
        }
        var motion = player.getDeltaMovement();
        var value = FlightTelemetry.measure(net.minecraft.util.Mth.lerp(partial, player.yo, player.getY()), ground, motion.x, motion.y, motion.z);
        if (DeploymentClientController.state() == 1) value = new FlightTelemetry(value.altitude(), DeploymentVehicleState.speed(), 0);
        float y = g.height / 2 - 120;
        gauge.render(g, g.width / 2 - 340, y, tr("altitude"), value.altitude(), 20, "m", true);
        gauge.render(g, g.width / 2 + 244, y, tr("speed"), value.speed(), 5, "m/s", false);
        g.text(Component.translatable("hud.blockzone.flight.descent", Double.isFinite(value.descent())
                        ? String.format(Locale.ROOT, "%.1f", value.descent()) : "N/A"),
                g.width / 2 + 272, y + 252, 12, HudCanvas.MUTED);
    }
    private static Component tr(String key) { return Component.translatable("hud.blockzone.flight." + key); }
}
