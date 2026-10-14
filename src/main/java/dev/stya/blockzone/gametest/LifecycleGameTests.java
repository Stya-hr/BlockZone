package dev.stya.blockzone.gametest;

import com.ptcrys.fpsmatch.core.capability.FPSMCapability;
import com.ptcrys.fpsmatch.core.capability.FPSMCapabilityManager;
import com.ptcrys.fpsmatch.core.capability.map.MapCapability;
import com.ptcrys.fpsmatch.core.data.AreaData;
import com.ptcrys.fpsmatch.core.map.BaseMap;
import dev.stya.blockzone.map.battlezone.BattlezoneMap;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("blockzone")
@PrefixGameTestTemplate(false)
public final class LifecycleGameTests {
    private static BattlezoneMap map(GameTestHelper helper) {
        var pos = helper.absolutePos(BlockPos.ZERO);
        return new BattlezoneMap(helper.getLevel(), "lifecycle_" + UUID.randomUUID(), new AreaData(pos, pos.offset(2, 2, 2)));
    }

    public static final class Probe extends MapCapability {
        int ticks, resets, destroys;
        public Probe(BaseMap map) { super(map); }
        @Override public void tick() { ticks++; }
        @Override public void reset() { resets++; }
        @Override public void destroy() { destroys++; }
    }

    @GameTest(template = "empty")
    public static void frameworkOwnsCapabilityTickResetAndRemoval(GameTestHelper helper) {
        if (!FPSMCapabilityManager.isRegistered(Probe.class))
            FPSMCapabilityManager.register(FPSMCapabilityManager.CapabilityType.MAP, Probe.class,
                    new FPSMCapability.Factory<BaseMap, Probe>() {
                        @Override public Probe create(BaseMap map) { return new Probe(map); }
                    });
        var map = map(helper);
        check(map.getCapabilityMap().add(Probe.class), "Attach probe capability");
        var probe = map.getCapabilityMap().get(Probe.class).orElseThrow();
        map.mapTick();
        check(probe.ticks == 1, "Framework ticks capability exactly once");
        map.reset();
        check(probe.resets == 1, "Map reset resets every attached capability exactly once");
        map.getCapabilityMap().remove(Probe.class);
        check(probe.destroys == 1, "Framework removal destroys capability exactly once");
        map.mapTick();
        check(probe.ticks == 1, "Removed capability no longer ticks");
        map.getCapabilityMap().clear();
        helper.succeed();
    }

    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
