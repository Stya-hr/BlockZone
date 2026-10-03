package dev.stya.blockzone.client.battlezone;

import dev.stya.blockzone.net.battlezone.BoundaryPreviewS2CPacket;
import dev.stya.blockzone.net.battlezone.ZoneStateS2CPacket;
import dev.stya.blockzone.util.battlezone.ZoneGeometry;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public final class ZoneClientState {
    private static volatile BoundaryPreviewS2CPacket preview;
    private static volatile Snapshot snapshot;
    private static volatile Snapshot previousSnapshot;
    private static volatile long lastUpdateTick;

    private ZoneClientState() {
    }

    public static void apply(ZoneStateS2CPacket packet) {
        if (!packet.boundaryVisible()) {
            snapshot = null;
            previousSnapshot = null;
            return;
        }
        Snapshot next = new Snapshot(packet.mapName(), packet.dimension(), packet.whiteoutActive(),
                packet.areaPos1().getX(), packet.areaPos1().getY(), packet.areaPos1().getZ(),
                packet.areaPos2().getX(), packet.areaPos2().getY(), packet.areaPos2().getZ(),
                packet.centerX(), packet.centerZ(), Math.max(0.0F, packet.radius()), packet.boundaryTexture());
        Snapshot current = snapshot;
        if (current == null || !current.dimension().equals(next.dimension())
                || !current.mapName().equals(next.mapName())) {
            previousSnapshot = next;
        } else {
            previousSnapshot = current;
        }
        snapshot = next;
        Minecraft minecraft = Minecraft.getInstance();
        lastUpdateTick = minecraft.level == null ? 0L : minecraft.level.getGameTime();
    }

    static void clear() {
        snapshot = null;
        previousSnapshot = null;
        preview = null;
    }

    public static void applyPreview(BoundaryPreviewS2CPacket packet) {
        preview = packet.visible() ? packet : null;
    }

    static BoundaryPreviewS2CPacket preview() {
        return preview;
    }

    static Snapshot current(float partialTick) {
        Snapshot target = snapshot;
        Snapshot previous = previousSnapshot;
        if (target == null || previous == null || target == previous) {
            return target;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return target;
        }
        float progress = Mth.clamp((minecraft.level.getGameTime() + partialTick - lastUpdateTick) / 5.0F, 0.0F, 1.0F);
        return new Snapshot(target.mapName(), target.dimension(), target.whiteoutActive(),
                target.x1(), target.y1(), target.z1(), target.x2(), target.y2(), target.z2(),
                Mth.lerp(progress, previous.centerX(), target.centerX()),
                Mth.lerp(progress, previous.centerZ(), target.centerZ()),
                Mth.lerp(progress, previous.radius(), target.radius()), target.boundaryTexture());
    }

    record Snapshot(String mapName, ResourceLocation dimension, boolean whiteoutActive,
                    int x1, int y1, int z1, int x2, int y2, int z2,
                    double centerX, double centerZ, float radius, String boundaryTexture) {
        double centerY() {
            return ZoneGeometry.centerY(y1, y2);
        }
    }
}
