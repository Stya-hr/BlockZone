package dev.stya.blockzone.command;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.ptcrys.fpsmatch.common.event.register.RegisterFPSMCommandEvent;
import com.ptcrys.fpsmatch.core.FPSMCore;
import dev.stya.blockzone.BlockZone;
import dev.stya.blockzone.map.battlezone.BattlezoneMap;
import dev.stya.blockzone.util.battlezone.PoisonPath;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.ArrayList;

@Mod.EventBusSubscriber(modid = BlockZone.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class PoisonSequenceCommands {
    private PoisonSequenceCommands() {}

    @SubscribeEvent public static void register(RegisterFPSMCommandEvent event) {
        var sequence = Commands.literal("sequence")
                .then(Commands.literal("list").executes(context -> edit(context, "list")))
                .then(Commands.literal("add").executes(context -> edit(context, "add")))
                .then(Commands.argument("sequence", IntegerArgumentType.integer(1))
                        .then(Commands.literal("get").executes(context -> edit(context, "get")))
                        .then(Commands.literal("remove").executes(context -> edit(context, "remove")))
                        .then(Commands.literal("circle")
                                .then(Commands.literal("add").then(circleArguments("circle_add")))
                                .then(Commands.argument("circle", IntegerArgumentType.integer(1))
                                        .then(Commands.literal("set").then(circleArguments("circle_set")))
                                        .then(Commands.literal("remove").executes(context -> edit(context, "circle_remove"))))));
        event.addChild(Commands.literal("map").then(Commands.literal("modify")
                .then(Commands.argument("game_type", StringArgumentType.string())
                        .then(Commands.argument("map_name", StringArgumentType.string())
                                .then(Commands.literal("settings").requires(source -> source.hasPermission(2)).then(sequence))))));
        event.registerHelp("fpsm map modify settings sequence", Component.literal(
                "Edit poison paths: list/add; <sequence> get/remove; <sequence> circle add <x> <z> <radius> <wait> <shrink> <damage_multiplier>; <sequence> circle <circle> set <same fields>/remove. Indices start at 1. Save with settings save."));
        event.registerParameters("fpsm map modify settings sequence", "*game_type", "*map_name", "list|add|<sequence> get|remove|circle ...");
    }

    private static ArgumentBuilder<CommandSourceStack, ?> circleArguments(String action) {
        return Commands.argument("x", DoubleArgumentType.doubleArg())
                .then(Commands.argument("z", DoubleArgumentType.doubleArg())
                        .then(Commands.argument("radius", DoubleArgumentType.doubleArg(0, 30_000_000))
                                .then(Commands.argument("wait", IntegerArgumentType.integer(0, 1_000_000))
                                        .then(Commands.argument("shrink", IntegerArgumentType.integer(0, 1_000_000))
                                                .then(Commands.argument("damage_multiplier", DoubleArgumentType.doubleArg(0, Float.MAX_VALUE))
                                                        .executes(context -> edit(context, action)))))));
    }

    private static int edit(CommandContext<CommandSourceStack> context, String action) {
        var source = context.getSource();
        var candidate = FPSMCore.getInstance().getMapByTypeWithName(
                StringArgumentType.getString(context, "game_type"), StringArgumentType.getString(context, "map_name")).orElse(null);
        if (!(candidate instanceof BattlezoneMap map)) {
            source.sendFailure(Component.literal("Specify a loaded Battlezone map."));
            return 0;
        }
        try {
            var paths = new ArrayList<>(map.configuredPoisonSequences());
            if (action.equals("list")) {
                for (int i = 0; i < paths.size(); i++) {
                    int number = i + 1;
                    int count = paths.get(i).circles().size();
                    source.sendSuccess(() -> Component.literal("Sequence " + number + ": " + count + " circles"), false);
                }
                return 1;
            }
            if (action.equals("add")) paths.add(map.defaultPoisonPath());
            else {
                int index = IntegerArgumentType.getInteger(context, "sequence") - 1;
                if (index >= paths.size()) throw new IllegalArgumentException("Sequence index must be 1.." + paths.size());
                if (action.equals("get")) {
                    var json = PoisonPath.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE, paths.get(index)).getOrThrow(false, message -> {});
                    source.sendSuccess(() -> Component.literal(json.toString()), false);
                    return 1;
                }
                if (action.equals("remove")) paths.remove(index);
                else {
                    var circles = new ArrayList<>(paths.get(index).circles());
                    int circleIndex = action.equals("circle_add") ? circles.size() : IntegerArgumentType.getInteger(context, "circle") - 1;
                    if (!action.equals("circle_add") && circleIndex >= circles.size())
                        throw new IllegalArgumentException("Circle index must be 1.." + circles.size());
                    if (action.equals("circle_remove")) circles.remove(circleIndex);
                    else {
                        var circle = new PoisonPath.Circle(DoubleArgumentType.getDouble(context, "x"),
                                DoubleArgumentType.getDouble(context, "z"), DoubleArgumentType.getDouble(context, "radius"),
                                IntegerArgumentType.getInteger(context, "wait"), IntegerArgumentType.getInteger(context, "shrink"),
                                DoubleArgumentType.getDouble(context, "damage_multiplier"));
                        if (action.equals("circle_add")) circles.add(circle); else circles.set(circleIndex, circle);
                    }
                    paths.set(index, new PoisonPath(circles));
                }
            }
            map.setPoisonSequences(paths);
            source.sendSuccess(() -> Component.literal("Updated poison sequences. Active matches keep their original path. Use settings save to persist."), false);
            return 1;
        } catch (IllegalArgumentException exception) {
            source.sendFailure(Component.literal(exception.getMessage()));
            return 0;
        }
    }
}
