package dev.stya.blockzone.client.equipment;

import com.mojang.blaze3d.platform.InputConstants;
import dev.stya.blockzone.BlockZone;
import dev.stya.blockzone.client.battlezone.DeploymentClientController;
import dev.stya.blockzone.client.hud.squad.SquadClientState;
import dev.stya.blockzone.registry.BlockzoneItems;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.IKeyConflictContext;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/** Uses the normal item-use protocol and ArmorPlateItem's server validation/40-tick consumption. */
@Mod.EventBusSubscriber(modid = BlockZone.MOD_ID, value = Dist.CLIENT)
public final class QuickPlating {
    private static boolean physicalDown;
    public static final KeyMapping KEY = new KeyMapping("key.blockzone.insert_plate", Context.INSTANCE,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, "key.categories.blockzone") {
        @Override public void setDown(boolean down) {
            super.setDown(down);
            physicalDown = down;
        }
    };
    private static int previousSlot = -1, plateSlot = -1;
    private static net.minecraft.client.player.LocalPlayer owner;
    private static boolean serverStarted;
    private static int waitingTicks;
    private static boolean blockedUntilRelease;
    private QuickPlating() {}
    private enum Context implements IKeyConflictContext {
        INSTANCE;
        @Override public boolean isActive() {
            return Minecraft.getInstance().screen == null && !DeploymentClientController.isAirborne()
                    && !SquadClientState.members().isEmpty();
        }
        @Override public boolean conflicts(IKeyConflictContext other) { return other == this; }
    }
    /** Only the vanilla use binding is held; no key state is changed or left pressed. */
    public static boolean holdsUse(KeyMapping mapping) {
        var mc = Minecraft.getInstance();
        return owner != null && owner == mc.player && mapping == mc.options.keyUse
                && KEY.isDown() && mc.screen == null && owner.getInventory().selected == plateSlot
                && owner.isUsingItem() && owner.getUseItem().is(BlockzoneItems.ARMOR_PLATE.get());
    }
    private static void finish(boolean cancel) {
        var mc = Minecraft.getInstance();
        if (owner != null && owner == mc.player) {
            if (cancel && owner.isUsingItem() && owner.getUseItem().is(BlockzoneItems.ARMOR_PLATE.get()) && mc.gameMode != null) mc.gameMode.releaseUsingItem(owner);
            if (owner.getInventory().selected == plateSlot) {
                owner.getInventory().selected = previousSlot;
                if (mc.gameMode != null) ((dev.stya.blockzone.mixin.client.CarriedItemSyncAccess)mc.gameMode).blockzone$syncCarriedItem();
            }
        }
        owner = null; previousSlot = -1; plateSlot = -1; serverStarted = false; waitingTicks = 0;
    }
    private static void begin() {
        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.gameMode == null || mc.screen != null || DeploymentClientController.isAirborne()
                || mc.player.isUsingItem() || mc.player.isSpectator()) return;
        var self = SquadClientState.members().stream().filter(m -> m.id().equals(mc.player.getUUID())).findFirst().orElse(null);
        if (self == null || !self.living() || self.rescueState() != 0 || self.plateSlots() == 0
                || self.armor() >= self.platePoints() * self.plateSlots()) return;
        for (int slot = 0; slot < 9; slot++) {
            var stack = mc.player.getInventory().getItem(slot);
            if (!stack.is(BlockzoneItems.ARMOR_PLATE.get()) || mc.player.getCooldowns().isOnCooldown(stack.getItem())) continue;
            owner = mc.player; previousSlot = owner.getInventory().selected; plateSlot = slot;
            owner.getInventory().selected = slot;
            var result = mc.gameMode.useItem(owner, InteractionHand.MAIN_HAND);
            if (!result.consumesAction()) finish(false);
            return;
        }
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var mc = Minecraft.getInstance();
        boolean held = KEY.isDown();
        while (KEY.consumeClick()) { /* Long presses use held state, never repeat-click events. */ }
        if (!physicalDown) blockedUntilRelease = false;
        else if (!Context.INSTANCE.isActive()) blockedUntilRelease = true;
        if (owner != null) {
            if (owner != mc.player || mc.screen != null || owner.isDeadOrDying()
                    || owner.getInventory().selected != plateSlot || mc.options.keyAttack.isDown()) {
                blockedUntilRelease = physicalDown;
                finish(true);
            } else {
                var status = SquadClientState.members().stream().filter(m -> m.id().equals(owner.getUUID())).findFirst().orElse(null);
                if (status == null || !status.living() || status.rescueState() != 0) {
                    blockedUntilRelease = physicalDown;
                    finish(true);
                } else if (!held && owner.isUsingItem()) finish(true);
                else {
                    serverStarted |= status.plating();
                    if (!owner.isUsingItem()) {
                        var info = mc.getConnection() == null ? null : mc.getConnection().getPlayerInfo(owner.getUUID());
                        int grace = 10 + (info == null ? 0 : Math.max(0, info.getLatency()) / 25);
                        // Wait for server completion; an early carried-item packet would cancel its last use tick.
                        if ((serverStarted && !status.plating()) || ++waitingTicks > grace) finish(false);
                    }
                }
            }
        }
        if (owner == null && held && !blockedUntilRelease && Context.INSTANCE.isActive()
                && !mc.options.keyAttack.isDown()) begin();
    }
    @Mod.EventBusSubscriber(modid = BlockZone.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        @SubscribeEvent public static void keys(RegisterKeyMappingsEvent event) { event.register(KEY); }
    }
}
