package dev.stya.blockzone.client.loot;

import dev.stya.blockzone.loot.LootDropEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.level.ClipContext;

/** The same widened, occluded target drives both use-item interaction and the HUD. */
public final class DropTargeting {
    private DropTargeting() { }

    private static net.minecraft.world.phys.AABB sightBounds(LootDropEntity drop) {
        return drop.getBoundingBox().move(0, 0.1, 0).inflate(0.2, 0.16, 0.2);
    }

    public static LootDropEntity target() {
        var mc = Minecraft.getInstance();
        return mc.hitResult instanceof EntityHitResult hit && hit.getEntity() instanceof LootDropEntity drop
                && drop.isAlive() ? drop : null;
    }

    public static void pick(float partial) {
        var mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.getCameraEntity() != mc.player || mc.player.isSpectator()) return;
        var eye = mc.player.getEyePosition(partial);
        var end = eye.add(mc.player.getViewVector(partial).scale(3));
        if (mc.hitResult instanceof EntityHitResult current && current.getEntity() instanceof LootDropEntity drop
                && (mc.player.distanceToSqr(drop) > 9 || mc.level.clip(new ClipContext(eye,
                sightBounds(drop).getCenter(), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player))
                .getType() != HitResult.Type.MISS)) {
            mc.hitResult = mc.player.pick(3, partial, false);
            mc.crosshairPickEntity = null;
        }
        double nearest = mc.hitResult == null || mc.hitResult.getType() == HitResult.Type.MISS
                ? 9 : eye.distanceToSqr(mc.hitResult.getLocation());
        for (var drop : mc.level.getEntitiesOfClass(LootDropEntity.class, mc.player.getBoundingBox().expandTowards(end.subtract(eye)).inflate(1))) {
            if (!drop.isAlive() || mc.player.distanceToSqr(drop) > 9) continue;
            var bounds = sightBounds(drop);
            var hit = bounds.clip(eye, end);
            if (hit.isEmpty() || eye.distanceToSqr(hit.get()) > nearest) continue;
            // Check the actual item as well as the enlarged ray box, so aim assistance cannot reach around walls.
            if (mc.level.clip(new ClipContext(eye, bounds.getCenter(), ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE, mc.player)).getType() != HitResult.Type.MISS) continue;
            nearest = eye.distanceToSqr(hit.get());
            mc.hitResult = new EntityHitResult(drop, hit.get());
            mc.crosshairPickEntity = drop;
        }
    }
}
