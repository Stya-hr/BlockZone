package dev.stya.blockzone.mixin.client;

import net.minecraft.client.gui.screens.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ChatScreen.class)
public interface ChatScreenInitialAccessor {
    @Accessor("initial") String blockzone$initial();
}
