package dev.stya.blockzone.command;

import dev.stya.blockzone.map.battlezone.BattlezoneMap;
import dev.stya.blockzone.net.battlezone.BattlezoneBoundaryPreviewS2CPacket;
import dev.stya.blockzone.net.battlezone.BattlezoneNetwork;
import dev.stya.blockzone.BlockZone;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.ptcrys.fpsmatch.common.event.register.RegisterFPSMCommandEvent;
import com.ptcrys.fpsmatch.core.FPSMCore;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BlockZone.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class BattlezoneMapCommands {
    private BattlezoneMapCommands() {
    }

    @SubscribeEvent
    public static void registerSnapshotCommand(RegisterFPSMCommandEvent event) {
        var saveSnapshot = Commands.literal("save")
                .executes(BattlezoneMapCommands::saveSnapshot);
        var snapshot = Commands.literal("snapshot")
                .then(saveSnapshot);
        var mapName = Commands.argument("map_name", StringArgumentType.string())
                .then(snapshot)
                .then(Commands.literal("boundary")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("show").executes(context -> showBoundary(context, true)))
                        .then(Commands.literal("hide").executes(context -> showBoundary(context, false))));
        var gameType = Commands.argument("game_type", StringArgumentType.string())
                .then(mapName);

        event.addChild(Commands.literal("map")
                .then(Commands.literal("modify")
                        .then(gameType)));

        event.registerHelp("fpsm map modify snapshot save",
                Component.literal("Save the scene snapshot for a Battlezone map."));
        event.registerParameters("fpsm map modify snapshot save", "*game_type", "*map_name");
        event.registerHelp("fpsm map modify boundary",
                Component.literal("Show or hide the Battlezone map boundary grid for this operator."));
        event.registerParameters("fpsm map modify boundary", "*game_type", "*map_name", "show|hide");
    }

    private static int showBoundary(CommandContext<CommandSourceStack> context, boolean visible) {
        String mapName = StringArgumentType.getString(context, "map_name");
        BattlezoneMap map = findMap(mapName);
        if (map == null) {
            context.getSource().sendFailure(Component.literal("No loaded Battlezone map named " + mapName + "."));
            return 0;
        }
        var player = context.getSource().getPlayer();
        if (player == null) {
            context.getSource().sendFailure(Component.literal("Run this command as a player."));
            return 0;
        }
        var area = map.getMapArea();
        BattlezoneNetwork.send(player, new BattlezoneBoundaryPreviewS2CPacket(
                map.getServerLevel().dimension().location(), visible, area.pos1(), area.pos2()));
        context.getSource().sendSuccess(() -> Component.literal(
                visible ? "Battlezone boundary preview enabled." : "Battlezone boundary preview disabled."), false);
        return 1;
    }

    private static BattlezoneMap findMap(String mapName) {
        return FPSMCore.getInstance()
                .getMapByTypeWithName(BattlezoneMap.GAME_TYPE, mapName)
                .filter(BattlezoneMap.class::isInstance)
                .map(BattlezoneMap.class::cast)
                .orElse(null);
    }

    private static int saveSnapshot(CommandContext<CommandSourceStack> context) {
        String mapName = StringArgumentType.getString(context, "map_name");
        BattlezoneMap map = findMap(mapName);
        if (map == null) {
            context.getSource().sendFailure(Component.literal("No loaded Battlezone map named " + mapName + "."));
            return 0;
        }
        if (map.isStart() && !map.isDebug()) {
            context.getSource().sendFailure(Component.literal("Stop the Battlezone match before saving its scene snapshot."));
            return 0;
        }
        if (!map.saveSceneSnapshot()) {
            context.getSource().sendFailure(Component.literal("Could not start saving the Battlezone scene snapshot."));
            return 0;
        }
        context.getSource().sendSuccess(
                () -> Component.literal("Started saving scene snapshot for Battlezone map " + mapName + "."), false);
        return 1;
    }
}
