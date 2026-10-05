package dev.stya.blockzone.loot;

import dev.stya.blockzone.map.battlezone.CombatRecovery;
import dev.stya.blockzone.map.battlezone.MatchRegeneration;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

public final class ArmorPlateItem extends Item {
    public ArmorPlateItem() { super(new Properties().stacksTo(16)); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isSpectator() || player.getAbsorptionAmount() >= CombatRecovery.MAX_ARMOR
                || (!level.isClientSide && MatchRegeneration.allowed(player))) {
            return InteractionResultHolder.fail(stack);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override public int getUseDuration(ItemStack stack) { return 40; }
    @Override public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.BOW; }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide && entity instanceof Player player && player.isAlive()
                && !MatchRegeneration.allowed(player) && player.getAbsorptionAmount() < CombatRecovery.MAX_ARMOR) {
            player.setAbsorptionAmount(CombatRecovery.insertPlate(player.getAbsorptionAmount()));
            if (!player.getAbilities().instabuild) stack.shrink(1);
            player.getCooldowns().addCooldown(this, 5);
        }
        return stack;
    }
}
