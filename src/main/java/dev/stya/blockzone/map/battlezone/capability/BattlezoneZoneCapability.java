package dev.stya.blockzone.map.battlezone.capability;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.ptcrys.fpsmatch.core.capability.FPSMCapability;
import com.ptcrys.fpsmatch.core.capability.FPSMCapabilityManager;
import com.ptcrys.fpsmatch.core.capability.map.MapCapability;
import com.ptcrys.fpsmatch.core.map.BaseMap;
import dev.stya.blockzone.command.ConfigurationCapabilityCommand;
import dev.stya.blockzone.map.battlezone.BattlezoneMap;
import dev.stya.blockzone.util.CodecSettings;
import dev.stya.blockzone.zone.PoisonPath;
import dev.stya.blockzone.zone.PoisonZoneController;
import java.util.List;

public final class BattlezoneZoneCapability extends MapCapability implements FPSMCapability.Savable<BattlezoneZoneCapability.Config> {
    public record Config(List<PoisonPath> poisonSequences, double poisonDamagePerSecond, String boundaryTexture) {
        public static final Codec<Config> CODEC = RecordCodecBuilder.create(i -> i.group(
                PoisonPath.CODEC.listOf().fieldOf("poisonSequences").forGetter(Config::poisonSequences),
                CodecSettings.NONNEGATIVE_DOUBLE.fieldOf("poisonDamagePerSecond").forGetter(Config::poisonDamagePerSecond),
                Codec.STRING.fieldOf("boundaryTexture").forGetter(Config::boundaryTexture)).apply(i, Config::new));
        public Config {
            poisonSequences = List.copyOf(poisonSequences);
            if (poisonSequences.isEmpty()) throw new IllegalArgumentException("Keep at least one sequence");
            if (!Double.isFinite(poisonDamagePerSecond) || poisonDamagePerSecond < 0)
                throw new IllegalArgumentException("Poison damage must be finite and nonnegative");
            if (net.minecraft.resources.ResourceLocation.tryParse(boundaryTexture) == null)
                throw new IllegalArgumentException("Invalid boundary texture resource location");
        }
        public Config withPaths(List<PoisonPath> paths) { return new Config(paths, poisonDamagePerSecond, boundaryTexture); }
    }
    private final PoisonZoneController controller;
    private Config configured;
    private Config active;
    public BattlezoneZoneCapability(BaseMap map) {
        super(map);
        if (!(map instanceof BattlezoneMap battlezone)) throw new IllegalArgumentException("Zone capability requires a Battlezone map");
        controller = new PoisonZoneController(battlezone);
        configured = defaults();
    }
    public Config defaults() {
        return new Config(List.of(((BattlezoneMap) map).defaultPoisonPath()), 1,
                "blockzone:textures/effect/battlezone_warning_fence.png");
    }
    public static void register() {
        FPSMCapabilityManager.register(FPSMCapabilityManager.CapabilityType.MAP, BattlezoneZoneCapability.class,
                new FPSMCapability.Factory<BaseMap, BattlezoneZoneCapability>() {
                    @Override public BattlezoneZoneCapability create(BaseMap map) { return new BattlezoneZoneCapability(map); }
                    @Override public Command command() {
                        return new ConfigurationCapabilityCommand<>("zone", BattlezoneZoneCapability.class, BattlezoneZoneCapability::defaults);
                    }
                });
    }
    @Override public Codec<Config> codec() { return Config.CODEC; }
    @Override public Config read() { return configured; }
    @Override public Config write(Config value) {
        for (int i = 0; i < value.poisonSequences().size(); i++) controller.resolvePath(value.poisonSequences().get(i), i + 1);
        configured = value;
        return configured;
    }
    public void begin() {
        controller.initialize(configured.poisonSequences(), configured.poisonDamagePerSecond());
        active = configured;
    }
    public String boundaryTexture() { return map.isStart() && active != null ? active.boundaryTexture() : configured.boundaryTexture(); }
    public PoisonZoneController controller() { return controller; }
    @Override public void reset() {
        active = null;
        var area = map.getMapArea();
        var path = defaults().poisonSequences().get(0);
        var first = path.circles().get(0);
        controller.reset(new dev.stya.blockzone.zone.ZoneGeometry(first.x(),
                dev.stya.blockzone.zone.ZoneGeometry.centerY(area.pos1().getY(), area.pos2().getY()), first.z(), first.radius()));
    }
    @Override public void destroy() { reset(); }
}
