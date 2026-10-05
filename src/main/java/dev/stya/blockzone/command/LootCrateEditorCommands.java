package dev.stya.blockzone.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.ptcrys.fpsmatch.common.event.register.RegisterFPSMCommandEvent;
import com.ptcrys.fpsmatch.core.FPSMCore;
import dev.stya.blockzone.BlockZone;
import dev.stya.blockzone.map.battlezone.BattlezoneMap;
import dev.stya.blockzone.net.editor.LootEditorSessions;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BlockZone.MOD_ID)
public final class LootCrateEditorCommands {
    private LootCrateEditorCommands() { }

    @SubscribeEvent
    public static void register(RegisterFPSMCommandEvent event) {
        var edit = Commands.literal("edit").executes(context -> {
            var source = context.getSource();
            var candidate = FPSMCore.getInstance().getMapByTypeWithName(
                    StringArgumentType.getString(context, "game_type"), StringArgumentType.getString(context, "map_name")).orElse(null);
            if (!(candidate instanceof BattlezoneMap map) || source.getPlayer() == null) {
                source.sendFailure(Component.translatable("editor.blockzone.loot.map_required"));
                return 0;
            }
            return LootEditorSessions.open(source.getPlayer(), map) ? 1 : 0;
        });
        event.addChild(Commands.literal("map").then(Commands.literal("modify")
                .then(Commands.argument("game_type", StringArgumentType.string())
                        .then(Commands.argument("map_name", StringArgumentType.string())
                                .then(Commands.literal("settings").requires(source -> source.hasPermission(2))
                                        .then(Commands.literal("loot").then(edit)))))));
        event.registerHelp("fpsm map modify settings loot edit", Component.translatable("editor.blockzone.loot.help"));
        event.registerParameters("fpsm map modify settings loot edit", "*game_type", "*map_name");
    }
}
