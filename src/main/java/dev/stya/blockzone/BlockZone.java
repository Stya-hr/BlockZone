package dev.stya.blockzone;

import net.minecraftforge.fml.common.Mod;
import dev.stya.blockzone.net.battlezone.BattlezoneNetwork;

@Mod(BlockZone.MOD_ID)
public final class BlockZone {
    public static final String MOD_ID = "blockzone";

    public BlockZone() {
        BattlezoneNetwork.register();
    }
}
