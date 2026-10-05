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
    public record Config(double matchHealth, double armorPlatePoints) {
        public static final Codec<Config> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.doubleRange(1, 1024).fieldOf("matchHealth").forGetter(Config::matchHealth),
                Codec.doubleRange(1, 1024).fieldOf("armorPlatePoints").forGetter(Config::armorPlatePoints)).apply(i, Config::new));
        public Config {
            if (!Double.isFinite(matchHealth) || matchHealth < 1 || matchHealth > 1024
                    || !Double.isFinite(armorPlatePoints) || armorPlatePoints < 1 || armorPlatePoints > 1024)
                throw new IllegalArgumentException("Health and plate points must be finite values in 1..1024");
        }
    }
    private Config configured = defaults();
    private final MatchCombatController controller = new MatchCombatController();
    public BattlezoneCombatCapability(BaseMap map) { super(map); }
    public static Config defaults() { return new Config(100, 50); }
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
    public void begin() { controller.start((float) configured.matchHealth(), (float) configured.armorPlatePoints()); }
    public MatchCombatController controller() { return controller; }
    @Override public void reset() { controller.clearRecovery(); }
    @Override public void destroy() { reset(); }
}
