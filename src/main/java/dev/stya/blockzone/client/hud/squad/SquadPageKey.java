package dev.stya.blockzone.client.hud.squad;

import com.mojang.blaze3d.platform.InputConstants;
import dev.stya.blockzone.BlockZone;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = BlockZone.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class SquadPageKey {
    public static final KeyMapping KEY = new KeyMapping("key.blockzone.squad_page", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_PAGE_DOWN, "key.categories.blockzone");
    @SubscribeEvent public static void register(RegisterKeyMappingsEvent event) { event.register(KEY); }
    private SquadPageKey() {}
}
