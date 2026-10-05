package dev.stya.blockzone.equipment;

import dev.stya.blockzone.combat.MatchRegeneration;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class ArmorExpansionItem extends Item {
    public ArmorExpansionItem() { super(new Properties().stacksTo(1)); }
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        var held = player.getItemInHand(hand);
        var chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (player.isSpectator() || !(chest.getItem() instanceof TacticalArmorItem carrier)
                || carrier.plateSlots() == 0 || PlateCapacity.expanded(chest)) return InteractionResultHolder.fail(held);
        if (!level.isClientSide) {
            PlateCapacity.setExpanded(chest, true);
            held.shrink(1);
            player.displayClientMessage(Component.translatable("message.blockzone.expansion_installed"), true);
            player.playSound(net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_LEATHER, 1, .9F);
        }
        return InteractionResultHolder.sidedSuccess(held, level.isClientSide);
    }
    public static void detach(net.minecraft.server.level.ServerPlayer player) {
        if (!player.isAlive() || player.isSpectator()) return;
        var chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!(chest.getItem() instanceof TacticalArmorItem carrier) || carrier.plateSlots() == 0
                || !PlateCapacity.expanded(chest)) return;
        PlateCapacity.setExpanded(chest, false);
        player.stopUsingItem();
        MatchRegeneration.map(player).filter(map -> map.isMatchActive())
                .ifPresent(map -> player.setAbsorptionAmount(Math.min(player.getAbsorptionAmount(),
                        map.getArmorPlatePoints() * carrier.plateSlots())));
        var attachment = new ItemStack(EquipmentRegistry.ARMOR_EXPANSION.get());
        if (!player.getInventory().add(attachment)) player.drop(attachment, false);
        player.displayClientMessage(Component.translatable("message.blockzone.expansion_removed"), true);
        player.playSound(net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_LEATHER, 1, .8F);
    }
}
