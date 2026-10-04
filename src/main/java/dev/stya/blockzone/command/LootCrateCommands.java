package dev.stya.blockzone.command;

import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.stya.blockzone.BlockZone;
import dev.stya.blockzone.loot.LootCrateAccess;
import dev.stya.blockzone.loot.LootCrateBlockEntity;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.storage.loot.LootDataType;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BlockZone.MOD_ID)
public final class LootCrateCommands {
    private LootCrateCommands() { }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("blockzone").requires(source -> source.hasPermission(2))
                .then(Commands.literal("lootcrate")
                        .then(Commands.literal("set")
                                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                        .then(Commands.argument("table", ResourceLocationArgument.id())
                                                .suggests((context, builder) -> SharedSuggestionProvider.suggestResource(
                                                        context.getSource().getServer().getLootData().getKeys(LootDataType.TABLE), builder))
                                                .executes(context -> configure(context, 0))
                                                .then(Commands.argument("seed", LongArgumentType.longArg())
                                                        .executes(context -> configure(context, LongArgumentType.getLong(context, "seed")))))))
                        .then(Commands.literal("reset")
                                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                        .executes(LootCrateCommands::reset)))));
    }

    private static LootCrateBlockEntity findCrate(CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        var source = context.getSource();
        var pos = BlockPosArgument.getLoadedBlockPos(context, "pos");
        if (!LootCrateAccess.mayEdit(source.getLevel(), pos)) {
            source.sendFailure(Component.translatable("blockzone.loot_crate.edit_locked"));
            return null;
        }
        if (source.getLevel().getBlockEntity(pos) instanceof LootCrateBlockEntity crate) return crate;
        source.sendFailure(Component.translatable("blockzone.loot_crate.not_found"));
        return null;
    }

    private static int configure(CommandContext<CommandSourceStack> context, long seed) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        var crate = findCrate(context);
        if (crate == null) return 0;
        var table = ResourceLocationArgument.getId(context, "table");
        if (!context.getSource().getServer().getLootData().getKeys(LootDataType.TABLE).contains(table)) {
            context.getSource().sendFailure(Component.translatable("blockzone.loot_crate.missing_table", table));
            return 0;
        }
        crate.configure(table, seed);
        context.getSource().sendSuccess(() -> Component.translatable("blockzone.loot_crate.configured", table, seed), true);
        return 1;
    }

    private static int reset(CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        var crate = findCrate(context);
        if (crate == null) return 0;
        crate.resetOpened();
        context.getSource().sendSuccess(() -> Component.translatable("blockzone.loot_crate.reset"), true);
        return 1;
    }
}
