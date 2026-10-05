package dev.stya.blockzone.command;

import com.google.gson.JsonParser;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.serialization.JsonOps;
import com.ptcrys.fpsmatch.common.command.FPSMCommand;
import com.ptcrys.fpsmatch.core.capability.FPSMCapability;
import dev.stya.blockzone.equipment.StartingLoadout;
import dev.stya.blockzone.map.battlezone.capability.BattlezoneLoadoutCapability;
import java.util.List;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public final class LoadoutCapabilityCommand implements FPSMCapability.Factory.Command {
    @Override public String getName() { return "loadout"; }

    @Override public LiteralArgumentBuilder<CommandSourceStack> builder(
            LiteralArgumentBuilder<CommandSourceStack> builder, CommandBuildContext context) {
        return builder.requires(source -> source.hasPermission(2))
                .then(Commands.literal("get").executes(c -> execute(c, "get")))
                .then(Commands.literal("set").then(Commands.argument("entries", StringArgumentType.greedyString())
                        .executes(c -> execute(c, "set"))))
                .then(Commands.literal("clear").executes(c -> execute(c, "clear")))
                .then(Commands.literal("reset").executes(c -> execute(c, "reset")));
    }

    private static int execute(CommandContext<CommandSourceStack> context, String action) {
        var capability = FPSMCommand.getMapCapability(context, BattlezoneLoadoutCapability.class);
        if (capability.isEmpty()) return 0;
        var loadout = capability.get();
        var source = context.getSource();
        if (action.equals("get")) {
            source.sendSuccess(() -> Component.literal(loadout.toJson().toString()), false);
            return 1;
        }
        try {
            var entries = switch (action) {
                case "set" -> loadout.codec().parse(JsonOps.INSTANCE,
                        JsonParser.parseString(StringArgumentType.getString(context, "entries")))
                        .getOrThrow(false, message -> {});
                case "clear" -> List.<StartingLoadout.Entry>of();
                case "reset" -> StartingLoadout.defaults();
                default -> throw new IllegalArgumentException("Unknown loadout operation");
            };
            dev.stya.blockzone.data.persistence.CapabilityConfiguration.save(loadout, entries);
        } catch (RuntimeException exception) {
            source.sendFailure(Component.literal("Invalid loadout: " + exception.getMessage()));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Loadout saved; applies to the next match."), false);
        return 1;
    }
}
