package dev.stya.blockzone;

import net.minecraftforge.fml.common.Mod;
import dev.stya.blockzone.net.battlezone.BattlezoneNetwork;
import dev.stya.blockzone.loot.LootCrateRegistry;
import dev.stya.blockzone.equipment.EquipmentRegistry;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(BlockZone.MOD_ID)
public final class BlockZone {
    public static final String MOD_ID = "blockzone";

    public BlockZone(FMLJavaModLoadingContext context) {
        LootCrateRegistry.register(context.getModEventBus());
        EquipmentRegistry.register(context.getModEventBus());
        BattlezoneNetwork.register();
        context.getModEventBus().addListener(this::commonSetup);
    }
    private void commonSetup(net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            dev.stya.blockzone.map.battlezone.capability.BattlezoneLoadoutCapability.register();
            dev.stya.blockzone.map.battlezone.capability.BattlezoneCombatCapability.register();
        });
    }
}
