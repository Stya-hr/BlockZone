package dev.stya.blockzone.combat;

import dev.stya.blockzone.map.battlezone.BattlezoneMap;
import dev.stya.blockzone.net.battlezone.BattlezoneNetwork;
import dev.stya.blockzone.net.battlezone.RescueStateS2CPacket;
import java.util.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** Server owns all rescue eligibility and clocks; clients only display snapshots. */
public final class DownedController {
    private static final UUID SLOW = UUID.fromString("cb99a1ce-0201-483e-958d-8bfb1f7eac62");
    private static final Map<ServerPlayer, Entry> DOWN = new HashMap<>();
    private static final Map<ServerPlayer, ServerPlayer> RESCUERS = new HashMap<>();
    private static final Map<ServerPlayer, net.minecraft.world.phys.Vec3> RESCUE_ORIGINS = new HashMap<>();
    private static final Set<ServerPlayer> FINISHING = new HashSet<>();
    private record Entry(BattlezoneMap map, RescueTimer timer, DamageSource source, long knockedTick) { }
    private DownedController() { }
    public static boolean down(ServerPlayer player) { return DOWN.containsKey(player); }
    public static boolean busy(ServerPlayer player) { return down(player) || RESCUERS.containsKey(player); }
    private static boolean eligible(ServerPlayer player, BattlezoneMap map) {
        return player.connection != null && player.serverLevel() == map.getServerLevel()
                && MatchRegeneration.map(player).orElse(null) == map
                && !MatchRegeneration.allowed(player) && player.isAlive();
    }
    private static List<ServerPlayer> teammates(ServerPlayer player, BattlezoneMap map) {
        return map.getMapTeams().getTeamByPlayer(player).map(team -> team.getOnline().stream()
                .filter(p -> p != player && eligible(p, map) && !down(p)).toList()).orElse(List.of());
    }
    public static boolean knock(ServerPlayer player, DamageSource source) {
        var map = MatchRegeneration.map(player).orElse(null);
        if (map == null || FINISHING.contains(player) || down(player)
                || map.getPhase() != BattlezoneMap.MatchPhase.MATCH
                || !eligible(player, map) || teammates(player, map).isEmpty()
                || source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) return false;
        DOWN.put(player, new Entry(map, new RescueTimer(), source, player.serverLevel().getGameTime()));
        cancelRescue(player);
        map.clearAirbornePlayer(player);
        player.closeContainer();
        player.stopUsingItem();
        player.setAbsorptionAmount(0);
        player.setHealth(Math.min(30, player.getMaxHealth()));
        player.setForcedPose(Pose.SWIMMING);
        var speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null && !speed.hasModifier(new AttributeModifier(SLOW, "Downed crawl", -.75,
                AttributeModifier.Operation.MULTIPLY_TOTAL)))
            speed.addTransientModifier(new AttributeModifier(SLOW, "Downed crawl", -.75,
                    AttributeModifier.Operation.MULTIPLY_TOTAL));
        sync(player, 1, RescueTimer.BLEED_TICKS, 0);
        return true;
    }
    public static boolean duplicateBulletDamage(ServerPlayer player, DamageSource source) {
        var entry = DOWN.get(player);
        return entry != null && entry.knockedTick == player.serverLevel().getGameTime()
                && source.getDirectEntity() != null && source.getDirectEntity() == entry.source.getDirectEntity();
    }
    public static void interrupted(ServerPlayer player) {
        var entry = DOWN.get(player);
        if (entry != null) { entry.timer.interrupt(); cancelRescue(player); }
        var target = RESCUERS.get(player);
        if (target != null) { DOWN.get(target).timer.interrupt(); cancelRescue(target); }
    }
    public static void clear(ServerPlayer player) {
        DOWN.remove(player);
        cancelRescue(player);
        var speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) speed.removeModifier(SLOW);
        player.setForcedPose(null);
        sync(player, 0, 0, 0);
    }
    private static void cancelRescue(ServerPlayer target) {
        for (var helper : List.copyOf(RESCUERS.keySet())) {
            if (helper == target || RESCUERS.get(helper) == target) {
                RESCUERS.remove(helper);
                RESCUE_ORIGINS.remove(helper);
                if (!down(helper)) { helper.setForcedPose(null); sync(helper, 0, 0, 0); }
            }
        }
    }
    private static boolean canRescue(ServerPlayer helper, ServerPlayer target, BattlezoneMap map) {
        return eligible(helper, map) && !down(helper) && helper.isShiftKeyDown()
                && !helper.isUsingItem() && helper.hurtTime == 0 && target.hurtTime == 0
                && helper.getViewVector(1).dot(target.getBoundingBox().getCenter()
                        .subtract(helper.getEyePosition()).normalize()) > .5
                && helper.distanceToSqr(target) <= 6.25
                && helper.hasLineOfSight(target)
                && helper.onGround()
                && helper.getDeltaMovement().horizontalDistanceSqr() < .0001
                && helper.position().subtract(RESCUE_ORIGINS.getOrDefault(helper, helper.position()))
                        .horizontalDistanceSqr() < .01;
    }
    public static void tick() {
        for (var player : List.copyOf(DOWN.keySet())) {
            var entry = DOWN.get(player);
            if (entry == null) continue;
            if (!eligible(player, entry.map) || entry.map.getPhase() != BattlezoneMap.MatchPhase.MATCH) {
                clear(player); continue;
            }
            player.setSprinting(false);
            player.stopUsingItem();
            player.setForcedPose(Pose.SWIMMING);
            var team = teammates(player, entry.map);
            var helper = RESCUERS.entrySet().stream().filter(e -> e.getValue() == player)
                    .map(Map.Entry::getKey).findFirst().orElse(null);
            if (helper != null && (!team.contains(helper) || !canRescue(helper, player, entry.map))) {
                cancelRescue(player); entry.timer.interrupt(); helper = null;
            }
            if (helper == null) helper = team.stream().filter(p -> !RESCUERS.containsKey(p))
                    .filter(p -> canRescue(p, player, entry.map)).findFirst().orElse(null);
            if (helper != null) {
                RESCUERS.put(helper, player);
                RESCUE_ORIGINS.putIfAbsent(helper, helper.position());
                helper.setForcedPose(Pose.CROUCHING);
                helper.setSprinting(false);
            }
            boolean expired = entry.timer.tick(helper != null);
            if (expired || team.isEmpty()) {
                clear(player);
                FINISHING.add(player);
                try { player.setHealth(1); player.invulnerableTime = 0; player.hurt(entry.source, Float.MAX_VALUE); }
                finally { FINISHING.remove(player); }
            } else if (entry.timer.rescued()) {
                clear(player);
                player.setHealth(player.getMaxHealth() * .3F);
                entry.map.combatHurt(player);
            } else {
                sync(player, 1, entry.timer.remaining(), entry.timer.progress());
                if (helper != null) sync(helper, 2, 0, entry.timer.progress());
            }
        }
    }
    public static void sync(ServerPlayer player, int state, int bleed, int progress) {
        BattlezoneNetwork.sendRescue(player, new RescueStateS2CPacket(player.getUUID(), state, bleed, progress));
    }
}
