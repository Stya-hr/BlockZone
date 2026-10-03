package dev.stya.blockzone.command;

import dev.stya.blockzone.map.battlezone.BattlezoneFlightRoute;
import dev.stya.blockzone.map.battlezone.BattlezoneMap;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.ptcrys.fpsmatch.common.event.register.RegisterFPSMCommandEvent;
import com.ptcrys.fpsmatch.core.FPSMCore;
import dev.stya.blockzone.BlockZone;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.ArrayList;
import java.util.Locale;

@Mod.EventBusSubscriber(modid = BlockZone.MOD_ID)
public final class BattlezoneRouteCommands {
    private BattlezoneRouteCommands() { }

    @SubscribeEvent
    public static void register(RegisterFPSMCommandEvent event) {
        var route = Commands.literal("route")
                .then(Commands.literal("list").executes(BattlezoneRouteCommands::list))
                .then(Commands.literal("add").executes(BattlezoneRouteCommands::add))
                .then(point("startpoint", true))
                .then(point("startpiont", true))
                .then(point("endpoint", false))
                .then(Commands.literal("speed").then(index().then(
                        Commands.argument("speed", DoubleArgumentType.doubleArg(0.1, 100))
                                .executes(context -> edit(context, "speed")))))
                .then(Commands.literal("remove").then(index().executes(context -> edit(context, "remove"))));
        // Singular spelling is the route editor; retain the existing plural settings commands.
        event.addChild(Commands.literal("map").then(Commands.literal("modify")
                .then(Commands.argument("game_type", StringArgumentType.string())
                        .then(Commands.argument("map_name", StringArgumentType.string())
                                .then(Commands.literal("setting").requires(source -> source.hasPermission(2))
                                        .then(route))))));
        event.registerHelp("fpsm map modify setting route",
                Component.literal("Edit deployment routes. Indexes start at 1; coordinates support ~ and ^. Edits are saved automatically."));
        event.registerParameters("fpsm map modify setting route", "*game_type", "*map_name",
                "add|list|remove <index>|startpoint <index> <x> <y> <z>|endpoint <index> <x> <y> <z>|speed <index> <blocks_per_second>");
    }

    private static RequiredArgumentBuilder<CommandSourceStack, Integer> index() {
        return Commands.argument("index", IntegerArgumentType.integer(1)).suggests((context, builder) -> {
            BattlezoneMap map = find(context);
            if (map != null) {
                for (int i = 1; i <= map.getDeploymentRoutes().size(); i++) {
                    String value = Integer.toString(i);
                    if (value.startsWith(builder.getRemaining())) {
                        builder.suggest(value);
                    }
                }
            }
            return builder.buildFuture();
        });
    }

    private static LiteralArgumentBuilder<CommandSourceStack> point(String name, boolean start) {
        return Commands.literal(name).then(index().then(
                Commands.argument("position", Vec3Argument.vec3(false))
                        .executes(context -> edit(context, start ? "start" : "end"))));
    }

    private static BattlezoneMap find(CommandContext<CommandSourceStack> context) {
        if (!BattlezoneMap.GAME_TYPE.equals(StringArgumentType.getString(context, "game_type"))) {
            return null;
        }
        return FPSMCore.getInstance().getMapByTypeWithName(BattlezoneMap.GAME_TYPE,
                        StringArgumentType.getString(context, "map_name"))
                .filter(BattlezoneMap.class::isInstance).map(BattlezoneMap.class::cast).orElse(null);
    }

    private static BattlezoneMap requireMap(CommandContext<CommandSourceStack> context, boolean editing) {
        BattlezoneMap map = find(context);
        if (map == null) {
            context.getSource().sendFailure(Component.literal("No loaded Battlezone map with that name and game type."));
        } else if (editing && map.isStart()) {
            context.getSource().sendFailure(Component.literal("Stop the match before editing deployment routes."));
            return null;
        }
        return map;
    }

    private static int list(CommandContext<CommandSourceStack> context) {
        BattlezoneMap map = requireMap(context, false);
        if (map == null) {
            return 0;
        }
        var routes = map.getDeploymentRoutes();
        if (routes.isEmpty()) {
            context.getSource().sendSuccess(() -> Component.literal("No configured routes; matches use an automatic route. Use setting route add."), false);
        }
        for (int i = 0; i < routes.size(); i++) {
            String message = describe(i + 1, routes.get(i))
                    + (map.isValidDeploymentRoute(routes.get(i)) ? "" : " [invalid: skipped at match start]");
            context.getSource().sendSuccess(() -> Component.literal(message), false);
        }
        return 1;
    }

    private static int add(CommandContext<CommandSourceStack> context) {
        BattlezoneMap map = requireMap(context, true);
        if (map == null) {
            return 0;
        }
        var routes = new ArrayList<>(map.getDeploymentRoutes());
        BattlezoneFlightRoute route = map.defaultDeploymentRoute();
        routes.add(route);
        map.setDeploymentRoutes(routes);
        context.getSource().sendSuccess(() -> Component.literal("Added and saved " + describe(routes.size(), route)), true);
        return routes.size();
    }

    private static int edit(CommandContext<CommandSourceStack> context, String operation) throws CommandSyntaxException {
        BattlezoneMap map = requireMap(context, true);
        if (map == null) {
            return 0;
        }
        int index = IntegerArgumentType.getInteger(context, "index");
        var routes = new ArrayList<>(map.getDeploymentRoutes());
        if (index > routes.size()) {
            context.getSource().sendFailure(Component.literal("Route index out of range. Use setting route list; indexes start at 1."));
            return 0;
        }
        BattlezoneFlightRoute old = routes.get(index - 1);
        BattlezoneFlightRoute updated = old;
        if (operation.equals("start") || operation.equals("end")) {
            if (context.getSource().getLevel() != map.getServerLevel()) {
                context.getSource().sendFailure(Component.literal("Set route points from the map's dimension."));
                return 0;
            }
            Vec3 position = Vec3Argument.getVec3(context, "position");
            if (!Double.isFinite(position.x) || !Double.isFinite(position.y) || !Double.isFinite(position.z)) {
                context.getSource().sendFailure(Component.literal("Route coordinates must be finite."));
                return 0;
            }
            updated = operation.equals("start")
                    ? new BattlezoneFlightRoute(position.x, position.y, position.z, old.endX(), old.endY(), old.endZ(), old.speed())
                    : new BattlezoneFlightRoute(old.startX(), old.startY(), old.startZ(), position.x, position.y, position.z, old.speed());
        } else if (operation.equals("speed")) {
            double speed = DoubleArgumentType.getDouble(context, "speed");
            if (!Double.isFinite(speed)) {
                context.getSource().sendFailure(Component.literal("Route speed must be finite."));
                return 0;
            }
            updated = new BattlezoneFlightRoute(old.startX(), old.startY(), old.startZ(), old.endX(), old.endY(), old.endZ(), speed);
        }
        if (operation.equals("remove")) {
            routes.remove(index - 1);
        } else {
            routes.set(index - 1, updated);
        }
        map.setDeploymentRoutes(routes);
        String message = operation.equals("remove") ? "Removed and saved route " + index + ". Remaining indexes shifted."
                : "Saved " + describe(index, updated) + (map.isValidDeploymentRoute(updated) ? ""
                : " [not ready: endpoints must share a height, be above the map by at least 16 blocks and lie inside its X/Z bounds]");
        context.getSource().sendSuccess(() -> Component.literal(message), true);
        return 1;
    }

    private static String describe(int index, BattlezoneFlightRoute route) {
        return String.format(Locale.ROOT, "route %d: (%.2f, %.2f, %.2f) -> (%.2f, %.2f, %.2f), %.2f blocks/s",
                index, route.startX(), route.startY(), route.startZ(), route.endX(), route.endY(), route.endZ(), route.speed());
    }
}
