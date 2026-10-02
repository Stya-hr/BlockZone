package dev.stya.blockzone.game.battlezone;

import com.ptcrys.fpsmatch.core.FPSMCore;
import com.ptcrys.fpsmatch.core.data.AreaData;
import dev.stya.blockzone.BlockZone;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BlockZone.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class BattlezoneBoundaryEnforcement {
    private static final double POSITION_EPSILON = 1.0E-5;

    private BattlezoneBoundaryEnforcement() {
    }

    @SubscribeEvent
    public static void keepPlayersInsideMap(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }

        FPSMCore.getInstance().getMapByPlayerWithSpec(player)
                .filter(BattlezoneMap.class::isInstance)
                .map(BattlezoneMap.class::cast)
                .filter(BattlezoneMap::isMatchActive)
                .ifPresent(map -> clampToMap(player, map.getMapArea()));
    }

    private static void clampToMap(ServerPlayer player, AreaData area) {
        if (area == null) {
            return;
        }

        BattlezoneBoundaryGeometry bounds = BattlezoneBoundaryGeometry.of(area.pos1().getX(), area.pos1().getZ(),
                area.pos2().getX(), area.pos2().getZ());
        double minX = bounds.minX();
        double maxX = bounds.maxX();
        double minZ = bounds.minZ();
        double maxZ = bounds.maxZ();
        double halfWidth = player.getBbWidth() * 0.5;
        double insetX = Math.min(halfWidth, (maxX - minX) * 0.5);
        double insetZ = Math.min(halfWidth, (maxZ - minZ) * 0.5);
        double clampedX = Mth.clamp(player.getX(), minX + insetX, maxX - insetX);
        double clampedZ = Mth.clamp(player.getZ(), minZ + insetZ, maxZ - insetZ);

        boolean clampX = Math.abs(clampedX - player.getX()) > POSITION_EPSILON;
        boolean clampZ = Math.abs(clampedZ - player.getZ()) > POSITION_EPSILON;
        if (!clampX && !clampZ) {
            return;
        }

        Vec3 movement = player.getDeltaMovement();
        player.setDeltaMovement(clampX ? 0.0 : movement.x, movement.y, clampZ ? 0.0 : movement.z);
        player.connection.teleport(clampedX, player.getY(), clampedZ, player.getYRot(), player.getXRot());
    }
}
