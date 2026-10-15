package dev.stya.blockzone.client.hud.weapon;

import dev.stya.blockzone.BlockZone;
import dev.stya.blockzone.mixin.client.CarriedItemSyncAccess;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Manual selection follows the five visible slots; QuickPlating still owns its temporary sixth slot. */
@Mod.EventBusSubscriber(modid = BlockZone.MOD_ID, value = Dist.CLIENT)
public final class WeaponSlotSelection {
    @SubscribeEvent public static void scroll(InputEvent.MouseScrollingEvent event) {
        var mc = Minecraft.getInstance();
        if (mc.screen != null || WeaponHud.inventory() == null || event.getScrollDelta() == 0) return;
        event.setCanceled(true);
        mc.player.getInventory().selected = next(mc.player.getInventory().selected, event.getScrollDelta());
        if (mc.gameMode != null) ((CarriedItemSyncAccess)mc.gameMode).blockzone$syncCarriedItem();
    }
    static int next(int selected, double delta) {
        return Math.floorMod(Math.min(4, Math.max(0, selected)) - (int)Math.signum(delta), 5);
    }
    @SubscribeEvent public static void keys(TickEvent.ClientTickEvent event) {
        var mc = Minecraft.getInstance();
        if (event.phase != TickEvent.Phase.START || mc.screen != null || WeaponHud.inventory() == null) return;
        // Consume before vanilla handleKeybinds runs; do not touch selected or the plating key's held state.
        for (int i = 5; i < mc.options.keyHotbarSlots.length; i++)
            while (mc.options.keyHotbarSlots[i].consumeClick()) { }
    }
    private WeaponSlotSelection() {}
}
