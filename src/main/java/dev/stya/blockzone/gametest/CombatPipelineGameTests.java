package dev.stya.blockzone.gametest;

import com.mojang.authlib.GameProfile;
import com.ptcrys.fpsmatch.core.damage.DamageSourceCategory;
import com.ptcrys.fpsmatch.core.damage.MinecraftDamageSourceClassifier;
import com.ptcrys.fpsmatch.core.map.DeathContext;
import dev.stya.blockzone.combat.BattlezoneDeathEvent;
import dev.stya.blockzone.combat.CombatDamageTypes;
import dev.stya.blockzone.combat.DownedDeathContext;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("blockzone")
@PrefixGameTestTemplate(false)
public final class CombatPipelineGameTests {
    private static ServerPlayer player(GameTestHelper helper) {
        return FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "combat_test"));
    }

    private static DeathContext context(ServerPlayer player, DamageSource source, ItemStack item) {
        return new DeathContext(player, null, source, item, player.serverLevel().getGameTime());
    }

    @GameTest(template = "empty")
    public static void poisonZoneIsDistinctAndUsesFrameworkClassification(GameTestHelper helper) {
        var source = CombatDamageTypes.poisonZone(helper.getLevel());
        check(source.is(CombatDamageTypes.POISON_ZONE), "Poison zone has its own registry ID");
        check(source.is(DamageTypeTags.BYPASSES_ARMOR), "Preserve magic damage armor behavior");
        check(MinecraftDamageSourceClassifier.classify(source) == DamageSourceCategory.ENVIRONMENT,
                "FPSMatch classifies the custom source");
        check(!helper.getLevel().damageSources().magic().is(CombatDamageTypes.POISON_ZONE),
                "Other magic damage must not become poison zone damage");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void bleedOutRestoresKnockDetailsAndConsumesThemOnce(GameTestHelper helper) {
        var player = player(helper);
        var source = helper.getLevel().damageSources().generic();
        var original = context(player, source, new ItemStack(Items.IRON_SWORD));
        original.setGunKill(true);
        original.setHeadShot(true);
        original.setPassWall(true);
        original.setPassSmoke(true);
        original.setScopedKill(true);
        var bullet = new net.minecraft.world.entity.projectile.Arrow(helper.getLevel(), 0, 1, 0);
        original.setGunBullet(bullet);
        DownedDeathContext.finishing(original, BattlezoneDeathEvent.Reason.BLEED_OUT);
        var finalized = context(player, source, ItemStack.EMPTY);
        check(DownedDeathContext.apply(finalized) == BattlezoneDeathEvent.Reason.BLEED_OUT, "Bleed-out reason");
        check(finalized.isGunKill() && finalized.isHeadShot() && finalized.isPassWall()
                && finalized.isPassSmoke() && finalized.isScopedKill(), "Original hit flags survive delayed death");
        check(finalized.getGunBullet() == bullet, "Original projectile survives delayed death");
        check(finalized.getDeathItem().is(Items.IRON_SWORD), "Original weapon survives switching weapons");
        original.getDeathItem().setCount(0);
        check(!finalized.getDeathItem().isEmpty(), "Finalized weapon is copied");
        check(DownedDeathContext.apply(context(player, source, ItemStack.EMPTY)) == BattlezoneDeathEvent.Reason.DIRECT,
                "Pending details are consumed once");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void finishingDetailsNeverOverwriteAnotherDamageSource(GameTestHelper helper) {
        var player = player(helper);
        var original = context(player, helper.getLevel().damageSources().generic(), new ItemStack(Items.IRON_SWORD));
        original.setHeadShot(true);
        DownedDeathContext.finishing(original, BattlezoneDeathEvent.Reason.TEAM_WIPE);
        var finisher = context(player, helper.getLevel().damageSources().fall(), new ItemStack(Items.STICK));
        check(DownedDeathContext.apply(finisher) == BattlezoneDeathEvent.Reason.DIRECT, "Another source is a direct kill");
        check(finisher.getDeathItem().is(Items.STICK) && !finisher.isHeadShot(), "Do not attribute finisher to knock hit");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void rescueCleanupAndCanceledDeathExpiryRemoveDetails(GameTestHelper helper) {
        var player = player(helper);
        var source = helper.getLevel().damageSources().generic();
        var original = context(player, source, new ItemStack(Items.IRON_SWORD));
        DownedDeathContext.finishing(original, BattlezoneDeathEvent.Reason.BLEED_OUT);
        DownedDeathContext.clear(player);
        check(DownedDeathContext.apply(context(player, source, ItemStack.EMPTY)) == BattlezoneDeathEvent.Reason.DIRECT,
                "Rescue, leave and reset cleanup remove attribution");
        DownedDeathContext.finishing(original, BattlezoneDeathEvent.Reason.TEAM_WIPE);
        DownedDeathContext.tick(player.serverLevel().getGameTime() + 6);
        check(DownedDeathContext.apply(context(player, source, ItemStack.EMPTY)) == BattlezoneDeathEvent.Reason.DIRECT,
                "A canceled elimination cannot contaminate a later death");
        helper.succeed();
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
