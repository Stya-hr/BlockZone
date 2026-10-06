package dev.stya.blockzone;

import net.minecraftforge.fml.common.Mod;
import dev.stya.blockzone.net.battlezone.BattlezoneNetwork;
import dev.stya.blockzone.registry.BlockzoneBlockEntities;
import dev.stya.blockzone.registry.BlockzoneBlocks;
import dev.stya.blockzone.registry.BlockzoneEntities;
import dev.stya.blockzone.registry.BlockzoneItems;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(BlockZone.MOD_ID)
public final class BlockZone {
    public static final String MOD_ID = "blockzone";

    public BlockZone(FMLJavaModLoadingContext context) {
        BlockzoneBlocks.register(context.getModEventBus());
        BlockzoneItems.register(context.getModEventBus());
        BlockzoneBlockEntities.register(context.getModEventBus());
        BlockzoneEntities.register(context.getModEventBus());
        context.getModEventBus().addListener(dev.stya.blockzone.resource.BlockzoneResources::addPackFinders);
        BattlezoneNetwork.register();
        if (net.minecraftforge.fml.ModList.get().isLoaded("tacz"))
            dev.stya.blockzone.combat.DownedGunEvents.register();
        context.getModEventBus().addListener(this::commonSetup);
    }
    private void commonSetup(net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            dev.stya.blockzone.map.battlezone.capability.BattlezoneLoadoutCapability.register();
            dev.stya.blockzone.map.battlezone.capability.BattlezoneCombatCapability.register();
            dev.stya.blockzone.map.battlezone.capability.BattlezoneZoneCapability.register();
        });
    }
}
