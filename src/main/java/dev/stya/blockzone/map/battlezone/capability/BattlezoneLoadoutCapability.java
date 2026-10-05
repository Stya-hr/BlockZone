package dev.stya.blockzone.map.battlezone.capability;

import com.mojang.serialization.Codec;
import com.ptcrys.fpsmatch.core.capability.FPSMCapability;
import com.ptcrys.fpsmatch.core.capability.FPSMCapabilityManager;
import com.ptcrys.fpsmatch.core.capability.map.MapCapability;
import com.ptcrys.fpsmatch.core.map.BaseMap;
import dev.stya.blockzone.command.LoadoutCapabilityCommand;
import dev.stya.blockzone.equipment.StartingLoadout;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;

/** Map configuration and the independently frozen equipment for its running match. */
public final class BattlezoneLoadoutCapability extends MapCapability
        implements FPSMCapability.Savable<List<StartingLoadout.Entry>> {
    private List<StartingLoadout.Entry> configured = StartingLoadout.defaults();
    private List<StartingLoadout.Prepared> active = List.of();
    private boolean matchActive;

    public BattlezoneLoadoutCapability(BaseMap map) { super(map); }

    public static void register() {
        FPSMCapabilityManager.register(FPSMCapabilityManager.CapabilityType.MAP,
                BattlezoneLoadoutCapability.class, new FPSMCapability.Factory<BaseMap, BattlezoneLoadoutCapability>() {
                    @Override public BattlezoneLoadoutCapability create(BaseMap map) {
                        return new BattlezoneLoadoutCapability(map);
                    }
                    @Override public Command command() { return new LoadoutCapabilityCommand(); }
                });
    }

    @Override public Codec<List<StartingLoadout.Entry>> codec() { return StartingLoadout.Entry.CODEC.listOf(); }
    @Override public List<StartingLoadout.Entry> read() { return configured; }
    @Override public List<StartingLoadout.Entry> write(List<StartingLoadout.Entry> entries) {
        var copy = List.copyOf(entries);
        StartingLoadout.prepare(copy); // Reject the whole update before changing configuration.
        configured = copy;
        return configured;
    }

    public List<StartingLoadout.Prepared> prepare() { return StartingLoadout.prepare(configured); }

    /** Activate only after BaseMap accepted the start, using the already validated stacks. */
    public void begin(List<StartingLoadout.Prepared> prepared) {
        active = prepared.stream().map(entry -> new StartingLoadout.Prepared(entry.slot(), entry.stack().copy())).toList();
        matchActive = true;
    }

    public void give(ServerPlayer player) {
        if (!matchActive || !map.isStart()) throw new IllegalStateException("Loadout match has not started");
        StartingLoadout.apply(player, active);
    }

    @Override public void reset() {
        active = List.of();
        matchActive = false;
    }
    @Override public void destroy() { reset(); }
}
