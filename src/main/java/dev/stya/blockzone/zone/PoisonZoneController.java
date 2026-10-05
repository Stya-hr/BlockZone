package dev.stya.blockzone.zone;

import dev.stya.blockzone.map.battlezone.BattlezoneMap;
import dev.stya.blockzone.net.battlezone.BattlezoneNetwork;
import dev.stya.blockzone.net.battlezone.ZonePreviewS2CPacket;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Resolves map configuration and connects zone progression to server damage and previews. */
public final class PoisonZoneController {
    private static final Logger LOGGER = LoggerFactory.getLogger(PoisonZoneController.class);
    private final BattlezoneMap map;
    private final PoisonZoneState state = new PoisonZoneState();
    private int sequenceNumber;

    public PoisonZoneController(BattlezoneMap map) { this.map = map; }

    public List<ZoneGeometry> resolvePath(PoisonPath path, int number) {
        var area = map.getMapArea();
        var bounds = BoundaryGeometry.of(area.pos1().getX(), area.pos1().getZ(), area.pos2().getX(), area.pos2().getZ());
        var result = path.resolve(bounds, ZoneGeometry.centerY(area.pos1().getY(), area.pos2().getY()));
        for (int i = 0; i < result.size(); i++) {
            var configured = path.circles().get(i);
            var actual = result.get(i);
            if (configured.x() != actual.centerX() || configured.z() != actual.centerZ()) {
                var warning = "Battlezone " + map.getMapName() + " sequence " + number + " circle " + i
                        + " exceeds map bounds; runtime center=(" + actual.centerX() + ", " + actual.centerZ()
                        + "). Radius and saved configuration unchanged.";
                LOGGER.warn(warning);
                map.getServerLevel().players().stream().filter(player -> player.hasPermissions(2))
                        .forEach(player -> player.sendSystemMessage(Component.literal(warning)));
            }
        }
        return result;
    }

    public void initialize(List<PoisonPath> paths, double baseDamage) {
        if (paths.isEmpty()) throw new IllegalArgumentException("Configure at least one poison sequence");
        var resolved = new ArrayList<List<ZoneGeometry>>();
        for (int i = 0; i < paths.size(); i++) resolved.add(resolvePath(paths.get(i), i + 1));
        sequenceNumber = map.getServerLevel().getRandom().nextInt(resolved.size()) + 1;
        state.initialize(paths.get(sequenceNumber - 1), resolved.get(sequenceNumber - 1), baseDamage);
    }

    public void tick() {
        state.tick();
        double damage = state.damage();
        if (damage <= 0 || map.getServerLevel().getGameTime() % 20 != 0) return;
        var zone = current();
        var damageSource = map.getServerLevel().damageSources().magic();
        for (var team : map.getMapTeams().getNormalTeams()) {
            for (var player : team.getOnline()) {
                if (team.getPlayerData(player.getUUID()).map(data -> !data.isLiving()).orElse(true)) continue;
                if (!map.hasDeploymentProtection(player) && !zone.contains(player.getX(), player.getY(), player.getZ()))
                    player.hurt(damageSource, (float) damage);
            }
        }
    }

    public boolean preview(ServerPlayer player, int number) {
        var paths = map.configuredPoisonSequences();
        if (number < 1 || number > paths.size()) return false;
        var circles = map.isMatchActive() && number == sequenceNumber ? state.sequence()
                : resolvePath(paths.get(number - 1), number);
        BattlezoneNetwork.send(player, new ZonePreviewS2CPacket(map.getServerLevel().dimension().location(), true, circles));
        player.sendSystemMessage(Component.literal("Sequence " + number + ": " + circles.size() + " circles (shape grid preview)"));
        for (int i = 0; i < circles.size(); i++) {
            var circle = circles.get(i);
            player.sendSystemMessage(Component.literal("Circle " + (i + 1) + ": X=" + circle.centerX() + ", Z="
                    + circle.centerZ() + ", shape=" + circle.shape().id() + ", radius=" + circle.radius()));
        }
        return true;
    }

    public void hidePreview(ServerPlayer player) {
        BattlezoneNetwork.send(player, new ZonePreviewS2CPacket(map.getServerLevel().dimension().location(), false, List.of()));
    }

    public ZoneGeometry current() {
        var area = map.getMapArea();
        return state.current(ZoneGeometry.centerY(area.pos1().getY(), area.pos2().getY()));
    }
    public ZoneGeometry initial() { return state.sequence().get(0); }
    public int sequenceNumber() { return sequenceNumber; }
    public void restart() { state.restart(); }
    public void reset(ZoneGeometry initial) { state.reset(initial); }
}
