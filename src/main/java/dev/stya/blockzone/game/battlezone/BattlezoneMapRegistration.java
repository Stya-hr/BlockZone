package dev.stya.blockzone.game.battlezone;

import dev.stya.blockzone.BlockZone;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.ptcrys.fpsmatch.common.event.register.RegisterFPSMapEvent;
import com.ptcrys.fpsmatch.common.event.register.RegisterFPSMCommandEvent;
import com.ptcrys.fpsmatch.core.FPSMCore;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BlockZone.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class BattlezoneMapRegistration {
    private BattlezoneMapRegistration() {
    }

    @SubscribeEvent
    public static void registerGameType(RegisterFPSMapEvent event) {
        event.registerGameType(BattlezoneMap.GAME_TYPE, BattlezoneMap::new);
    }

    @SubscribeEvent
    public static void registerSnapshotCommand(RegisterFPSMCommandEvent event) {
        var saveSnapshot = Commands.literal("save")
                .executes(BattlezoneMapRegistration::saveSnapshot);
        var snapshot = Commands.literal("snapshot")
                .then(saveSnapshot);
        var mapName = Commands.argument("map_name", StringArgumentType.string())
                .then(snapshot);
        var gameType = Commands.argument("game_type", StringArgumentType.string())
                .then(mapName);

        event.addChild(Commands.literal("map")
                .then(Commands.literal("modify")
                        .then(gameType)));

        event.registerHelp("fpsm map modify snapshot save",
                Component.literal("Save the scene snapshot for a Battlezone map."));
        event.registerParameters("fpsm map modify snapshot save", "*game_type", "*map_name");
    }

    private static int saveSnapshot(CommandContext<CommandSourceStack> context) {
        String mapName = StringArgumentType.getString(context, "map_name");
        BattlezoneMap map = FPSMCore.getInstance()
                .getMapByTypeWithName(BattlezoneMap.GAME_TYPE, mapName)
                .filter(BattlezoneMap.class::isInstance)
                .map(BattlezoneMap.class::cast)
                .orElse(null);
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
