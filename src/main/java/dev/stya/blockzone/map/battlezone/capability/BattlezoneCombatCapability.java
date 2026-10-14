package dev.stya.blockzone.map.battlezone.capability;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.ptcrys.fpsmatch.core.capability.FPSMCapability;
import com.ptcrys.fpsmatch.core.capability.FPSMCapabilityManager;
import com.ptcrys.fpsmatch.core.capability.map.MapCapability;
import com.ptcrys.fpsmatch.core.map.BaseMap;
import dev.stya.blockzone.command.ConfigurationCapabilityCommand;
import dev.stya.blockzone.combat.MatchCombatController;

public final class BattlezoneCombatCapability extends MapCapability implements FPSMCapability.Savable<BattlezoneCombatCapability.Config> {
    public record Config(double matchHealth, double armorPlatePoints, double downedHealth,
                         double bleedSeconds, double rescueSeconds, double rescueDistance,
                         double reviveHealthFraction, double downedSpeedFraction,
                         double recoveryDelaySeconds, double recoveryIntervalSeconds, double recoveryHealthFraction) {
        public static final Codec<Config> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.doubleRange(1, 1024).fieldOf("matchHealth").forGetter(Config::matchHealth),
                Codec.doubleRange(1, 1024).fieldOf("armorPlatePoints").forGetter(Config::armorPlatePoints),
                Codec.doubleRange(1, 1024).fieldOf("downedHealth").forGetter(Config::downedHealth),
                Codec.doubleRange(0.05, 3600).fieldOf("bleedSeconds").forGetter(Config::bleedSeconds),
                Codec.doubleRange(0.05, 3600).fieldOf("rescueSeconds").forGetter(Config::rescueSeconds),
                Codec.doubleRange(0.1, 64).fieldOf("rescueDistance").forGetter(Config::rescueDistance),
                Codec.doubleRange(0.001, 1).fieldOf("reviveHealthFraction").forGetter(Config::reviveHealthFraction),
                Codec.doubleRange(0, 1).fieldOf("downedSpeedFraction").forGetter(Config::downedSpeedFraction),
                Codec.doubleRange(0, 3600).fieldOf("recoveryDelaySeconds").forGetter(Config::recoveryDelaySeconds),
                Codec.doubleRange(0.05, 3600).fieldOf("recoveryIntervalSeconds").forGetter(Config::recoveryIntervalSeconds),
                Codec.doubleRange(0.001, 1).fieldOf("recoveryHealthFraction").forGetter(Config::recoveryHealthFraction)).apply(i, Config::new));
        public Config {
            requireRange("matchHealth", matchHealth, 1, 1024);
            requireRange("armorPlatePoints", armorPlatePoints, 1, 1024);
            requireRange("downedHealth", downedHealth, 1, 1024);
            requireRange("bleedSeconds", bleedSeconds, 0.05, 3600);
            requireRange("rescueSeconds", rescueSeconds, 0.05, 3600);
            requireRange("rescueDistance", rescueDistance, 0.1, 64);
            requireRange("reviveHealthFraction", reviveHealthFraction, 0.001, 1);
            requireRange("downedSpeedFraction", downedSpeedFraction, 0, 1);
            requireRange("recoveryDelaySeconds", recoveryDelaySeconds, 0, 3600);
            requireRange("recoveryIntervalSeconds", recoveryIntervalSeconds, 0.05, 3600);
            requireRange("recoveryHealthFraction", recoveryHealthFraction, 0.001, 1);
        }
        private static void requireRange(String name, double value, double min, double max) {
            if (!Double.isFinite(value) || value < min || value > max)
                throw new IllegalArgumentException(name + " must be finite and in " + min + ".." + max);
        }
        public static int ticks(double seconds) { return (int) Math.ceil(seconds * 20); }
    }
    private Config configured = defaults();
    private final MatchCombatController controller = new MatchCombatController();
    public BattlezoneCombatCapability(BaseMap map) { super(map); }
    public static Config defaults() { return new Config(100, 50, 50, 60, 8, 2.5, .3, .25, 5, .25, .05); }
    public static void register() {
        FPSMCapabilityManager.register(FPSMCapabilityManager.CapabilityType.MAP, BattlezoneCombatCapability.class,
                new FPSMCapability.Factory<BaseMap, BattlezoneCombatCapability>() {
                    @Override public BattlezoneCombatCapability create(BaseMap map) { return new BattlezoneCombatCapability(map); }
                    @Override public Command command() {
                        return new ConfigurationCapabilityCommand<>("combat", BattlezoneCombatCapability.class, cap -> defaults());
                    }
                });
    }
    @Override public Codec<Config> codec() { return Config.CODEC; }
    @Override public Config read() { return configured; }
    @Override public Config write(Config value) { configured = java.util.Objects.requireNonNull(value); return configured; }
    public void begin() { controller.start(configured); }
    public MatchCombatController controller() { return controller; }
    @Override public void tick() {
        var battlezone = (dev.stya.blockzone.map.battlezone.BattlezoneMap) map;
        for (var player : map.getMapTeams().getOnlineWithSpec()) {
            controller.tickRecovery(player);
            if (!dev.stya.blockzone.combat.MatchRegeneration.allowed(player)
                    && player.getAbsorptionAmount() > controller.maxArmor(player))
                player.setAbsorptionAmount(controller.maxArmor(player));
        }
        dev.stya.blockzone.combat.DownedController.tick(battlezone);
        dev.stya.blockzone.combat.DownedDeathContext.tick(battlezone);
    }
    @Override public void reset() {
        controller.clearRecovery();
        if (map instanceof dev.stya.blockzone.map.battlezone.BattlezoneMap battlezone)
            dev.stya.blockzone.combat.DownedController.clear(battlezone);
    }
    @Override public void destroy() { reset(); }
}
