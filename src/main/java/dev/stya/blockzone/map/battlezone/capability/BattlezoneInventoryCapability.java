package dev.stya.blockzone.map.battlezone.capability;

import com.mojang.serialization.Codec;
import com.ptcrys.fpsmatch.core.capability.FPSMCapability;
import com.ptcrys.fpsmatch.core.capability.FPSMCapabilityManager;
import com.ptcrys.fpsmatch.core.capability.map.MapCapability;
import com.ptcrys.fpsmatch.core.map.BaseMap;
import dev.stya.blockzone.command.InventoryCapabilityCommand;
import dev.stya.blockzone.inventory.InventoryRules;
import dev.stya.blockzone.inventory.InventorySlot;
import dev.stya.blockzone.inventory.PanelInventory;
import dev.stya.blockzone.inventory.SlotRule;
import java.util.Map;
import java.util.Objects;

/** Reuses FPSMatch persistence; each running match freezes its own admission rules. */
public final class BattlezoneInventoryCapability extends MapCapability implements FPSMCapability.Savable<InventoryRules> {
    private InventoryRules configured = InventoryRules.defaults();
    private Map<InventorySlot, SlotRule> configuredRules = configured.compile();
    private Map<InventorySlot, SlotRule> activeRules;

    public BattlezoneInventoryCapability(BaseMap map) { super(map); }

    public static void register() {
        FPSMCapabilityManager.register(FPSMCapabilityManager.CapabilityType.MAP, BattlezoneInventoryCapability.class,
                new FPSMCapability.Factory<BaseMap, BattlezoneInventoryCapability>() {
                    @Override public BattlezoneInventoryCapability create(BaseMap map) {
                        return new BattlezoneInventoryCapability(map);
                    }
                    @Override public Command command() { return new InventoryCapabilityCommand(); }
                });
    }

    @Override public Codec<InventoryRules> codec() { return InventoryRules.CODEC; }
    @Override public InventoryRules read() { return configured; }
    @Override public InventoryRules write(InventoryRules value) {
        var compiled = Objects.requireNonNull(value).compile();
        configured = value;
        configuredRules = compiled;
        return configured;
    }

    public void begin() { activeRules = configuredRules; }

    public SlotRule rule(InventorySlot slot) {
        return (activeRules == null ? configuredRules : activeRules).get(Objects.requireNonNull(slot));
    }

    public PanelInventory createInventory() { return new PanelInventory(this::rule); }

    @Override public void reset() { activeRules = null; }
    @Override public void destroy() { reset(); }
}
