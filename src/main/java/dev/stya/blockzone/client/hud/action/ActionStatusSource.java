package dev.stya.blockzone.client.hud.action;

import dev.stya.blockzone.client.battlezone.RescueClientState;
import dev.stya.blockzone.client.equipment.QuickPlating;
import dev.stya.blockzone.client.hud.common.HudCanvas;
import dev.stya.blockzone.client.hud.common.HudProgress;
import dev.stya.blockzone.registry.BlockzoneItems;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.EntityHitResult;

/** Adapts existing rescue snapshots and native item-use clocks without inventing client operations. */
final class ActionStatusSource {
    static ActionStatus read(float partial) {
        var mc = Minecraft.getInstance();
        var player = mc.player;
        var rescue = RescueClientState.snapshot(player.getUUID());
        if (rescue != null) {
            if (rescue.state() == 2 || rescue.rescueTicks() > 0) {
                return new ActionStatus(tr(rescue.state() == 2 ? "rescuing" : "receiving_rescue"), tr("hold_position"),
                        rescue.state() == 2 ? mc.options.keyShift : null,
                        HudProgress.elapsed(rescue.rescueTicks(), rescue.rescueTotalTicks(), 20), HudCanvas.GREEN);
            }
            return new ActionStatus(tr("downed"), tr("bleeding"), null,
                    HudProgress.remaining(rescue.bleedTicks(), rescue.bleedTotalTicks(), 20), HudCanvas.RED);
        }
        if (player.isUsingItem()) {
            var item = player.getUseItem();
            int duration = item.getUseDuration();
            if (duration > 0 && duration <= 1200) {
                boolean plate = item.is(BlockzoneItems.ARMOR_PLATE.get());
                return new ActionStatus(plate ? tr("plating") : item.getHoverName(), tr("hold_use"),
                        plate && QuickPlating.KEY.isDown() ? QuickPlating.KEY : mc.options.keyUse,
                        HudProgress.elapsed(player.getTicksUsingItem() + partial, duration, 20), plate ? HudCanvas.GREEN : HudCanvas.TEXT);
            }
        }
        if (mc.hitResult instanceof EntityHitResult hit && hit.getEntity() instanceof net.minecraft.world.entity.player.Player target
                && target.isAlive() && player.isAlliedTo(target) && player.hasLineOfSight(target)) {
            var state = RescueClientState.snapshot(target.getUUID());
            if (state != null && state.state() == 1 && player.distanceToSqr(target) <= state.rescueDistance() * state.rescueDistance())
                return new ActionStatus(tr("rescuing"), Component.translatable("hud.blockzone.action.rescue_ready", target.getDisplayName(),
                        String.format(java.util.Locale.ROOT, "%.1f", state.rescueTotalTicks() / 20f)), mc.options.keyShift, null, HudCanvas.GREEN);
        }
        return null;
    }
    static Component tr(String key) { return Component.translatable("hud.blockzone.action." + key); }
    private ActionStatusSource() {}
}
