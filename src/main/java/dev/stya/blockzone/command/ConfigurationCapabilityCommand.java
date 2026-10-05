package dev.stya.blockzone.command;

import com.google.gson.JsonParser;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.serialization.JsonOps;
import com.ptcrys.fpsmatch.common.command.FPSMCommand;
import com.ptcrys.fpsmatch.core.capability.FPSMCapability;
import com.ptcrys.fpsmatch.core.capability.map.MapCapability;
import dev.stya.blockzone.data.persistence.CapabilityConfiguration;
import java.util.function.Function;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/** Shared get/set/reset commands for server-side map configuration capabilities. */
public class ConfigurationCapabilityCommand<T, C extends MapCapability & FPSMCapability.Savable<T>> implements FPSMCapability.Factory.Command {
    private final String name;
    private final Class<C> type;
    private final Function<C, T> defaults;
    public ConfigurationCapabilityCommand(String name, Class<C> type, Function<C, T> defaults) {
        this.name = name; this.type = type; this.defaults = defaults;
    }
    @Override public String getName() { return name; }
    @Override public LiteralArgumentBuilder<CommandSourceStack> builder(LiteralArgumentBuilder<CommandSourceStack> builder, CommandBuildContext context) {
        return builder.requires(source -> source.hasPermission(2))
                .then(Commands.literal("get").executes(c -> execute(c, "get")))
                .then(Commands.literal("set").then(Commands.argument("config", StringArgumentType.greedyString()).executes(c -> execute(c, "set"))))
                .then(Commands.literal("reset").executes(c -> execute(c, "reset")));
    }
    private int execute(CommandContext<CommandSourceStack> context, String action) {
        var capability = FPSMCommand.getMapCapability(context, type);
        if (capability.isEmpty()) return 0;
        var cap = capability.get();
        var source = context.getSource();
        if (action.equals("get")) {
            source.sendSuccess(() -> Component.literal(cap.toJson().toString()), false);
            return 1;
        }
        try {
            T value = action.equals("reset") ? defaults.apply(cap) : cap.codec().parse(JsonOps.INSTANCE,
                    JsonParser.parseString(StringArgumentType.getString(context, "config"))).getOrThrow(false, message -> {});
            CapabilityConfiguration.save(cap, value);
            source.sendSuccess(() -> Component.literal(name + " configuration saved; applies to the next match."), false);
            return 1;
        } catch (RuntimeException failure) {
            source.sendFailure(Component.literal("Cannot save " + name + ": " + failure.getMessage()));
            return 0;
        }
    }
}
