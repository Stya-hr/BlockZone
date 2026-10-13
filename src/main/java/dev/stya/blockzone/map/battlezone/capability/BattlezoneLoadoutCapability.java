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
    private boolean readingSavedConfiguration;

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
    @Override public void decode(com.google.gson.JsonElement json) {
        boolean previous = readingSavedConfiguration;
        readingSavedConfiguration = true;
        try { write(codec().parse(com.mojang.serialization.JsonOps.INSTANCE, json).getOrThrow(false, message -> {})); }
        finally { readingSavedConfiguration = previous; }
    }
    @Override public List<StartingLoadout.Entry> read() { return configured; }
    @Override public List<StartingLoadout.Entry> write(List<StartingLoadout.Entry> entries) {
        var copy = List.copyOf(entries);
        // The inventory capability may be decoded later. Validate structure/types now, cross-check after restore at start.
        var permissive = dev.stya.blockzone.inventory.SlotRule.parse(List.of("*"), 1024);
        StartingLoadout.prepare(copy, readingSavedConfiguration ? slot -> permissive : configuredRules());
        configured = copy;
        return configured;
    }

    public List<StartingLoadout.Prepared> prepare() { return StartingLoadout.prepare(configured, configuredRules()); }
    private java.util.function.Function<dev.stya.blockzone.inventory.InventorySlot, dev.stya.blockzone.inventory.SlotRule> configuredRules() {
        var rules = map == null ? dev.stya.blockzone.inventory.InventoryRules.defaults()
                : com.ptcrys.fpsmatch.core.capability.CapabilityMap.getMapCapability(map, BattlezoneInventoryCapability.class)
                    .map(BattlezoneInventoryCapability::read).orElseGet(dev.stya.blockzone.inventory.InventoryRules::defaults);
        return rules.compile()::get;
    }

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
