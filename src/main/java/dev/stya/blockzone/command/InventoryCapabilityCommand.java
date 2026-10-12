package dev.stya.blockzone.command;

import com.google.gson.JsonParser;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.serialization.JsonOps;
import com.ptcrys.fpsmatch.common.command.FPSMCommand;
import dev.stya.blockzone.data.persistence.CapabilityConfiguration;
import dev.stya.blockzone.inventory.InventoryRules;
import dev.stya.blockzone.inventory.InventorySlot;
import dev.stya.blockzone.map.battlezone.capability.BattlezoneInventoryCapability;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/** Shared whole-config operations plus a single-slot update that preserves all other rules. */
public final class InventoryCapabilityCommand extends ConfigurationCapabilityCommand<InventoryRules, BattlezoneInventoryCapability> {
    public InventoryCapabilityCommand() {
        super("inventory", BattlezoneInventoryCapability.class, cap -> InventoryRules.defaults());
    }

    @Override public LiteralArgumentBuilder<CommandSourceStack> builder(
            LiteralArgumentBuilder<CommandSourceStack> builder, CommandBuildContext context) {
        return super.builder(builder, context).then(Commands.literal("slot")
                .then(Commands.argument("slot", StringArgumentType.word())
                        .suggests((c, suggestions) -> {
                            InventorySlot.ALL.forEach(slot -> suggestions.suggest(slot.id()));
                            return suggestions.buildFuture();
                        })
                        .then(Commands.argument("rule", StringArgumentType.greedyString()).executes(c -> {
                            var capability = FPSMCommand.getMapCapability(c, BattlezoneInventoryCapability.class);
                            if (capability.isEmpty()) return 0;
                            try {
                                var slot = InventorySlot.byId(StringArgumentType.getString(c, "slot"));
                                var definition = InventoryRules.Definition.CODEC.parse(JsonOps.INSTANCE,
                                        JsonParser.parseString(StringArgumentType.getString(c, "rule")))
                                        .getOrThrow(false, message -> {});
                                CapabilityConfiguration.save(capability.get(), capability.get().read().withSlot(slot, definition));
                                c.getSource().sendSuccess(() -> Component.literal("Inventory slot saved; applies to the next match."), false);
                                return 1;
                            } catch (RuntimeException failure) {
                                c.getSource().sendFailure(Component.literal("Cannot save inventory slot: " + failure.getMessage()));
                                return 0;
                            }
                        }))));
    }
}
