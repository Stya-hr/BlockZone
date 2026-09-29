package dev.stya.blockzone.game.battlezone;

import dev.stya.blockzone.BlockZone;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.ptcrys.fpsmatch.common.event.register.RegisterFPSMapEvent;
import com.ptcrys.fpsmatch.common.event.register.RegisterFPSMCommandEvent;
import com.ptcrys.fpsmatch.core.FPSMCore;
import net.minecraft.commands.Commands;
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
        event.getTree().then(Commands.literal("battlezone")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("snapshot")
                        .then(Commands.literal("save")
                                .then(Commands.argument("map", StringArgumentType.word())
                                        .suggests((context, builder) -> net.minecraft.commands.SharedSuggestionProvider.suggest(
                                                FPSMCore.getInstance().getMapNamesWithType(BattlezoneMap.GAME_TYPE), builder))
                                        .executes(context -> {
                                            String mapName = StringArgumentType.getString(context, "map");
                                            BattlezoneMap map = FPSMCore.getInstance()
                                                    .getMapByTypeWithName(BattlezoneMap.GAME_TYPE, mapName)
                                                    .filter(BattlezoneMap.class::isInstance)
                                                    .map(BattlezoneMap.class::cast)
                                                    .orElse(null);
                                            if (map == null) {
                                                context.getSource().sendFailure(Component.literal("No loaded Battlezone map named " + mapName + "."));
                                                return 0;
                                            }
                                            if (map.isStart()) {
                                                context.getSource().sendFailure(Component.literal("Stop the Battlezone match before saving its scene snapshot."));
                                                return 0;
                                            }
                                            if (!map.saveSceneSnapshot()) {
                                                context.getSource().sendFailure(Component.literal("Failed to save the Battlezone scene snapshot."));
                                                return 0;
                                            }
                                            context.getSource().sendSuccess(
                                                    () -> Component.literal("Saved scene snapshot for Battlezone map " + mapName + "."), false);
                                            return 1;
                                        })))));
    }
}
