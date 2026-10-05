package dev.stya.blockzone.resource;

import dev.stya.blockzone.BlockZone;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.fml.ModList;

/** One bundled pack for client assets and, when TaCZ is installed, its weapon loot table. */
public final class BlockzoneResources {
    private BlockzoneResources() { }

    public static void addPackFinders(AddPackFindersEvent event) {
        if (event.getPackType() == PackType.SERVER_DATA && !ModList.get().isLoaded("tacz")) return;
        var root = ModList.get().getModFileById(BlockZone.MOD_ID).getFile()
                .findResource("resourcepacks", "blockzone");
        event.addRepositorySource(consumer -> {
            var pack = Pack.readMetaAndCreate("builtin/blockzone", Component.translatable("pack.blockzone.title"),
                    true, id -> new PathPackResources(id, root, true), event.getPackType(),
                    Pack.Position.BOTTOM, PackSource.BUILT_IN);
            if (pack == null) throw new IllegalStateException("Missing BlockZone built-in pack metadata");
            consumer.accept(pack);
        });
    }
}
