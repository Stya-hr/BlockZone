package dev.stya.blockzone.equipment;

import dev.stya.blockzone.inventory.InventorySlot;
import dev.stya.blockzone.inventory.PlayerPanelInventory;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.items.IItemHandlerModifiable;

/** Optional Curios bridge. All third-party API references stay behind the mod-loaded boundary. */
public final class AccessoryCompat {
    public static final String SLOT = "blockzone_accessory";
    public static boolean loaded() { return ModList.get().isLoaded("curios"); }
    public static boolean isAccessory(ItemStack stack) {
        return !stack.isEmpty() && (stack.getItem() instanceof AccessoryItem || loaded() && Curios.isAccessory(stack));
    }
    public static Optional<IItemHandlerModifiable> inventory(Player player) {
        return loaded() ? Curios.inventory(player) : Optional.empty();
    }
    public static void requireSlots(Player player) {
        if (loaded() && inventory(player).filter(handler -> handler.getSlots() >= 2).isEmpty())
            throw new IllegalArgumentException("Curios blockzone_accessory must provide two slots for the player");
    }
    public static Optional<net.minecraft.nbt.ListTag> captureOriginal(Player player) {
        return loaded() ? Curios.captureOriginal(player) : Optional.empty();
    }
    public static boolean restoreOriginal(Player player, net.minecraft.nbt.ListTag snapshot) {
        return loaded() && Curios.restoreOriginal(player, snapshot);
    }
    public static boolean canEquip(Player player, int index, ItemStack stack) {
        if (!isAccessory(stack)) return false;
        if (stack.getItem() instanceof AccessoryItem item && !item.canEquip(player, index, stack)) return false;
        return !loaded() || Curios.canEquip(player, index, stack);
    }
    public static boolean canUnequip(Player player, int index, ItemStack stack) {
        if (stack.getItem() instanceof AccessoryItem item && !item.canUnequip(player, index, stack)) return false;
        return !loaded() || Curios.canUnequip(player, index, stack);
    }
    public static void register() { if (loaded()) Curios.register(); }

    private static final class Curios {
        static boolean isAccessory(ItemStack stack) {
            return top.theillusivec4.curios.api.CuriosApi.getCurio(stack).isPresent()
                    || stack.getTags().anyMatch(tag -> tag.location().getNamespace().equals("curios"));
        }
        static Optional<IItemHandlerModifiable> inventory(Player player) {
            return top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player).resolve()
                    .flatMap(handler -> handler.getStacksHandler(SLOT))
                    .map(handler -> (IItemHandlerModifiable) handler.getStacks());
        }
        static Optional<net.minecraft.nbt.ListTag> captureOriginal(Player player) {
            return top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player).resolve()
                    .map(handler -> handler.saveInventory(true).copy());
        }
        static boolean restoreOriginal(Player player, net.minecraft.nbt.ListTag snapshot) {
            var handler = top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player).resolve();
            handler.ifPresent(inventory -> inventory.loadInventory(snapshot.copy()));
            return handler.isPresent();
        }
        static top.theillusivec4.curios.api.SlotContext context(Player player, int index) {
            return new top.theillusivec4.curios.api.SlotContext(SLOT, player, index, false, true);
        }
        static boolean canEquip(Player player, int index, ItemStack stack) {
            return top.theillusivec4.curios.api.CuriosApi.getCurio(stack).resolve()
                    .map(curio -> curio.canEquip(context(player, index))).orElse(true);
        }
        static boolean canUnequip(Player player, int index, ItemStack stack) {
            return top.theillusivec4.curios.api.CuriosApi.getCurio(stack).resolve()
                    .map(curio -> curio.canUnequip(context(player, index))).orElse(true);
        }
        static void register() {
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(Curios::dropRules);
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(net.minecraftforge.eventbus.api.EventPriority.LOWEST, Curios::equipEvent);
            top.theillusivec4.curios.api.CuriosApi.registerCurioPredicate(
                    ResourceLocation.fromNamespaceAndPath("blockzone", "accessory"), result -> {
                        var context = result.slotContext();
                        if (!(context.entity() instanceof Player player) || context.index() < 0 || context.index() >= 2) return false;
                        var slot = InventorySlot.ACCESSORIES.get(context.index());
                        return PlayerPanelInventory.get(player).filter(PlayerPanelInventory::active)
                                .map(data -> data.accepts(slot, result.stack()) && canEquip(player, context.index(), result.stack())).orElse(false);
                    });
            net.minecraftforge.registries.ForgeRegistries.ITEMS.getValues().forEach(item -> {
                if (!(item instanceof AccessoryItem accessory)) return;
                top.theillusivec4.curios.api.CuriosApi.registerCurio(item, new top.theillusivec4.curios.api.type.capability.ICurioItem() {
                    @Override public boolean canEquip(top.theillusivec4.curios.api.SlotContext context, ItemStack stack) {
                        return context.entity() instanceof Player player && accessory.canEquip(player, context.index(), stack);
                    }
                    @Override public boolean canUnequip(top.theillusivec4.curios.api.SlotContext context, ItemStack stack) {
                        return !(context.entity() instanceof Player player) || accessory.canUnequip(player, context.index(), stack);
                    }
                    @Override public void curioTick(top.theillusivec4.curios.api.SlotContext context, ItemStack stack) {
                        if (context.entity() instanceof Player player) accessory.accessoryTick(player, context.index(), stack);
                    }
                    @Override public void onEquip(top.theillusivec4.curios.api.SlotContext context, ItemStack previous, ItemStack stack) {
                        if (context.entity() instanceof Player player) accessory.onEquip(player, context.index(), stack);
                    }
                    @Override public void onUnequip(top.theillusivec4.curios.api.SlotContext context, ItemStack next, ItemStack stack) {
                        if (context.entity() instanceof Player player) accessory.onUnequip(player, context.index(), stack);
                    }
                });
            });
        }
        static void equipEvent(top.theillusivec4.curios.api.event.CurioEquipEvent event) {
            var context = event.getSlotContext();
            if (!(context.entity() instanceof Player player)) return;
            PlayerPanelInventory.get(player).filter(PlayerPanelInventory::active).ifPresent(data -> {
                if (!context.identifier().equals(SLOT) || context.cosmetic() || context.index() < 0 || context.index() >= 2
                        || event.getStack().getCount() != 1 || !data.accepts(InventorySlot.ACCESSORIES.get(context.index()), event.getStack()))
                    event.setResult(net.minecraftforge.eventbus.api.Event.Result.DENY);
            });
        }
        static void dropRules(top.theillusivec4.curios.api.event.DropRulesEvent event) {
            if (!(event.getEntity() instanceof Player player)) return;
            PlayerPanelInventory.get(player).filter(PlayerPanelInventory::active).ifPresent(data -> {
                var owned = InventorySlot.ACCESSORIES.stream().map(data::getStack).filter(stack -> !stack.isEmpty()).toList();
                event.addOverride(stack -> owned.stream().anyMatch(item -> item == stack),
                        top.theillusivec4.curios.api.type.capability.ICurio.DropRule.ALWAYS_KEEP);
            });
        }
    }
    private AccessoryCompat() {}
}
