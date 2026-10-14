package dev.stya.blockzone.combat;

import com.ptcrys.fpsmatch.common.event.FPSMGunDamageEvent;
import com.ptcrys.fpsmatch.compat.PassThroughFlagResolver;
import com.ptcrys.fpsmatch.core.map.DeathContext;
import dev.stya.blockzone.BlockZone;
import dev.stya.blockzone.map.battlezone.BattlezoneMap;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Keeps framework gun details across the longer downed interval, without another death pipeline. */
@Mod.EventBusSubscriber(modid = BlockZone.MOD_ID)
public final class DownedDeathContext {
    private record GunHit(BattlezoneMap map, FPSMGunDamageEvent event, long tick) { }
    private record Pending(BattlezoneMap map, DeathContext context, BattlezoneDeathEvent.Reason reason, long tick) { }
    private static final Map<ServerPlayer, GunHit> GUN_HITS = new HashMap<>();
    private static final Map<ServerPlayer, Pending> PENDING = new HashMap<>();
    private DownedDeathContext() { }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void gunDamage(FPSMGunDamageEvent event) {
        if (event.getBaseAmount() > 0 && event.getHurtEntity() instanceof ServerPlayer player
                && !MatchRegeneration.allowed(player))
            GUN_HITS.put(player, new GunHit(MatchRegeneration.map(player).orElseThrow(), event, player.serverLevel().getGameTime()));
    }

    public static DeathContext capture(BattlezoneMap map, ServerPlayer player, DamageSource source) {
        var attacker = map.getAttackerFromDamageSource(source).orElse(null);
        var context = new DeathContext(player, attacker, source,
                map.resolveDeathItem(attacker, source).copy(), player.serverLevel().getGameTime());
        var hit = GUN_HITS.remove(player);
        // TaCZ's two damage portions share a bullet. Never attach a recent unrelated gun hit.
        if (hit != null && hit.tick == context.getCreatedTick() && hit.event.getBullet() != null
                && hit.event.getBullet() == source.getDirectEntity()) {
            var flags = PassThroughFlagResolver.fromBulletAndTarget(hit.event.getBullet(), player);
            context.setGunKill(true);
            context.setHeadShot(hit.event.isHeadShot());
            context.setGunBullet(hit.event.getBullet());
            context.setPassWall(flags.passWall());
            context.setPassSmoke(flags.passSmoke());
            context.setScopedKill(flags.scoped());
        }
        return context;
    }

    public static void finishing(DeathContext context, BattlezoneDeathEvent.Reason reason) {
        var player = context.getDeadPlayer();
        PENDING.put(player, new Pending(MatchRegeneration.map(player).orElse(null), context, reason, player.serverLevel().getGameTime()));
    }

    /** Called only by FPSMatch's finalized handleDeath, before mode cleanup. */
    public static BattlezoneDeathEvent.Reason apply(DeathContext context) {
        var pending = PENDING.remove(context.getDeadPlayer());
        if (pending == null || context.getCreatedTick() - pending.tick > 5
                || pending.context.getDamageSource() != context.getDamageSource())
            return BattlezoneDeathEvent.Reason.DIRECT;
        var original = pending.context;
        context.setDeathItem(original.getDeathItem().copy());
        context.setGunKill(original.isGunKill());
        context.setHeadShot(original.isHeadShot());
        context.setGunBullet(original.getGunBullet());
        context.setPassWall(original.isPassWall());
        context.setPassSmoke(original.isPassSmoke());
        context.setScopedKill(original.isScopedKill());
        return pending.reason;
    }

    @SubscribeEvent
    public static void stopped(net.minecraftforge.event.server.ServerStoppedEvent event) {
        GUN_HITS.clear();
        PENDING.clear();
    }

    public static void clear(ServerPlayer player) {
        GUN_HITS.remove(player);
        PENDING.remove(player);
    }

    public static void clear(BattlezoneMap map) {
        GUN_HITS.entrySet().removeIf(e -> e.getValue().map == map);
        PENDING.entrySet().removeIf(e -> e.getValue().map == map);
    }

    public static void tick(BattlezoneMap map) {
        long tick = map.getServerLevel().getGameTime();
        GUN_HITS.entrySet().removeIf(e -> e.getValue().map == map && tick - e.getValue().tick > 1);
        PENDING.entrySet().removeIf(e -> e.getValue().map == map && tick - e.getValue().tick > 5);
    }

    public static void tick(long tick) {
        GUN_HITS.entrySet().removeIf(e -> tick - e.getValue().tick > 1);
        // Canceled deaths must not contaminate a later elimination.
        PENDING.entrySet().removeIf(e -> tick - e.getValue().tick > 5);
    }
}
