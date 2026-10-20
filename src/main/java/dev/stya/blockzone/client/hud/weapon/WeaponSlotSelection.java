package dev.stya.blockzone.client.hud.weapon;

import dev.stya.blockzone.BlockZone;
import dev.stya.blockzone.inventory.PlayerPanelInventory;
import dev.stya.blockzone.mixin.client.CarriedItemSyncAccess;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/** Manual selection follows the five visible slots; QuickPlating still owns its temporary sixth slot. */
@Mod.EventBusSubscriber(modid = BlockZone.MOD_ID, value = Dist.CLIENT)
public final class WeaponSlotSelection {
    private static final KeyMapping[] ITEM_KEYS = {
            key("key.blockzone.select_throwable", GLFW.GLFW_KEY_Z),
            key("key.blockzone.select_tactical_one", GLFW.GLFW_KEY_X),
            key("key.blockzone.select_tactical_two", GLFW.GLFW_KEY_C)
    };

    private static KeyMapping key(String name, int key) {
        return new KeyMapping(name, KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, key,
                "key.categories.blockzone.gameplay");
    }

    public static KeyMapping itemKey(int slot) { return ITEM_KEYS[slot - 2]; }

    private static boolean active(Minecraft mc) {
        return mc.player != null && mc.player.isAlive() && !mc.player.isSpectator()
                && PlayerPanelInventory.get(mc.player).filter(PlayerPanelInventory::active).isPresent();
    }

    @SubscribeEvent public static void scroll(InputEvent.MouseScrollingEvent event) {
        var mc = Minecraft.getInstance();
        if (mc.screen != null || !active(mc) || event.getScrollDelta() == 0) return;
        event.setCanceled(true);
        mc.player.getInventory().selected = next(mc.player.getInventory().selected, event.getScrollDelta());
        if (mc.gameMode != null) ((CarriedItemSyncAccess)mc.gameMode).blockzone$syncCarriedItem();
    }
    static int next(int selected, double delta) {
        return Math.floorMod(Math.min(4, Math.max(0, selected)) - (int)Math.signum(delta), 5);
    }
    @SubscribeEvent public static void keys(TickEvent.ClientTickEvent event) {
        var mc = Minecraft.getInstance();
        if (event.phase == TickEvent.Phase.END && active(mc)
                && mc.player.getInventory().selected != 0 && mc.player.getMainHandItem().isEmpty()) {
            mc.player.getInventory().selected = 0;
            if (mc.gameMode != null) ((CarriedItemSyncAccess)mc.gameMode).blockzone$syncCarriedItem();
        }
        if (event.phase != TickEvent.Phase.START) return;
        boolean active = mc.screen == null && active(mc);
        for (int i = 0; i < ITEM_KEYS.length; i++) {
            while (ITEM_KEYS[i].consumeClick()) {
                if (active) select(mc, i + 2);
            }
        }
        if (!active) return;
        // Consume before vanilla handleKeybinds runs; do not touch selected or the plating key's held state.
        for (int i = 5; i < mc.options.keyHotbarSlots.length; i++)
            while (mc.options.keyHotbarSlots[i].consumeClick()) { }
    }

    private static void select(Minecraft mc, int slot) {
        if (mc.player.getInventory().selected == slot) return;
        mc.player.getInventory().selected = slot;
        if (mc.gameMode != null) ((CarriedItemSyncAccess)mc.gameMode).blockzone$syncCarriedItem();
    }

    @Mod.EventBusSubscriber(modid = BlockZone.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        @SubscribeEvent public static void register(RegisterKeyMappingsEvent event) {
            for (var key : ITEM_KEYS) event.register(key);
        }
    }
    private WeaponSlotSelection() {}
}
