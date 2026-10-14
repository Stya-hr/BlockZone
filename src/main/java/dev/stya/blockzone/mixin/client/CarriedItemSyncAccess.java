package dev.stya.blockzone.mixin.client;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Keep the native carried-item cache and its packet in sync after restoring the previous weapon. */
@Mixin(MultiPlayerGameMode.class)
public interface CarriedItemSyncAccess {
    @Invoker("ensureHasSentCarriedItem")
    void blockzone$syncCarriedItem();
}
