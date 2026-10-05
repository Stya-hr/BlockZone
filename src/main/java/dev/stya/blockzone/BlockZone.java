package dev.stya.blockzone;

import net.minecraftforge.fml.common.Mod;
import dev.stya.blockzone.net.battlezone.BattlezoneNetwork;
import dev.stya.blockzone.loot.LootCrateRegistry;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(BlockZone.MOD_ID)
public final class BlockZone {
    public static final String MOD_ID = "blockzone";

    public BlockZone(FMLJavaModLoadingContext context) {
        LootCrateRegistry.register(context.getModEventBus());
        BattlezoneNetwork.register();
    }
}
