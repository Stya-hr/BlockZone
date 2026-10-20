package dev.stya.blockzone.gametest;

import com.ptcrys.fpsmatch.core.data.AreaData;
import dev.stya.blockzone.loot.MatchDrops;
import dev.stya.blockzone.map.battlezone.BattlezoneMap;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import java.util.UUID;

@GameTestHolder("blockzone")
@PrefixGameTestTemplate(false)
public final class MatchDropGameTests {
    @GameTest(template = "empty")
    public static void conversionWaitsForSpawnCallerAndIgnoresFakeItems(GameTestHelper helper) throws Exception {
        var pos = helper.absolutePos(BlockPos.ZERO);
        var map = new BattlezoneMap(helper.getLevel(), "pending_drop_" + UUID.randomUUID(), new AreaData(pos, pos.offset(2, 2, 2)));
        // Isolate the entity conversion boundary without starting deployment or restoring a scene.
        var started = com.ptcrys.fpsmatch.core.map.BaseMap.class.getDeclaredField("isStart");
        started.setAccessible(true);
        started.setBoolean(map, true);
        var round = BattlezoneMap.class.getDeclaredField("lootRoundId");
        round.setAccessible(true);
        round.set(map, UUID.randomUUID());
        var core = com.ptcrys.fpsmatch.core.FPSMCore.getInstance();
        core.registerMap(map.getGameType(), map);
        var original = new ItemEntity(helper.getLevel(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1, new ItemStack(Items.PAPER, 3));
        helper.getLevel().addFreshEntity(original);
        var owner = UUID.randomUUID();
        original.setTarget(owner); // Vanilla /give sets these after addFreshEntity returns.
        original.setPickUpDelay(73);
        var fake = new ItemEntity(helper.getLevel(), original.getX(), original.getY(), original.getZ(), new ItemStack(Items.PAPER));
        helper.getLevel().addFreshEntity(fake);
        fake.makeFakeItem();
        helper.runAfterDelay(2, () -> {
            try {
                var converted = helper.getLevel().getEntity(original.getUUID());
                helper.assertTrue(converted instanceof dev.stya.blockzone.loot.LootDropEntity,
                        "Real vanilla item converts under its original UUID");
                var tag = converted.saveWithoutId(new CompoundTag());
                helper.assertTrue(tag.getUUID("Owner").equals(owner) && tag.getShort("PickupDelay") > 60,
                        "Conversion retains changes made after entity insertion");
                helper.assertTrue(!(helper.getLevel().getEntity(fake.getUUID()) instanceof dev.stya.blockzone.loot.LootDropEntity),
                        "Visual pickup effect never becomes a real drop");
                converted.discard();
                helper.succeed();
            } finally {
                core.getAllMaps().get(map.getGameType()).remove(map);
                map.getCapabilityMap().clear();
                original.discard();
                fake.discard();
            }
        });
    }

    @GameTest(template = "empty")
    public static void conversionPreservesStackAndPickupRestrictions(GameTestHelper helper) {
        var pos = helper.absolutePos(BlockPos.ZERO);
        var map = new BattlezoneMap(helper.getLevel(), "drop_" + UUID.randomUUID(), new AreaData(pos, pos.offset(2, 2, 2)));
        var stack = new ItemStack(Items.DIAMOND_SWORD, 1);
        stack.getOrCreateTag().putString("Identity", "match_drop");
        stack.enchant(net.minecraft.world.item.enchantment.Enchantments.SHARPNESS, 2);
        var original = new ItemEntity(helper.getLevel(), pos.getX(), pos.getY() + 1, pos.getZ(), stack);
        original.setPickUpDelay(37);
        original.setTarget(UUID.randomUUID());
        original.setThrower(UUID.randomUUID());
        original.setDeltaMovement(.1, .2, -.3);
        var drop = MatchDrops.copy(original, map);
        var before = original.saveWithoutId(new CompoundTag());
        var after = drop.saveWithoutId(new CompoundTag());
        for (var key : java.util.List.of("Item", "PickupDelay", "Owner", "Thrower", "Motion", "Pos", "UUID"))
            helper.assertTrue(java.util.Objects.equals(before.get(key), after.get(key)), "Conversion must retain " + key);
        helper.assertTrue(drop.belongsToMap(map.getMapName()), "Converted item is bound to its match");
        helper.assertTrue(drop.getPickRadius() > original.getPickRadius(), "Match item has a wider interaction ray");
        helper.assertTrue(after.getShort("Age") == -32768, "Match drops remain scattered without merging or despawning");
        var frameworkDrop = new com.ptcrys.fpsmatch.common.entity.MatchDropEntity(helper.getLevel(), stack.copy());
        frameworkDrop.setPos(original.position());
        frameworkDrop.setDeltaMovement(original.getDeltaMovement());
        var converted = MatchDrops.copy(frameworkDrop, map);
        var saved = converted.saveWithoutId(new CompoundTag());
        helper.assertTrue(ItemStack.isSameItemSameTags(converted.getItem(), stack), "FPSMatch drops retain their complete stack");
        helper.assertTrue(saved.getShort("Health") == 5 && saved.getShort("PickupDelay") == 20,
                "FPSMatch conversion initializes vanilla item health and pickup delay");
        helper.assertTrue(converted.position().equals(frameworkDrop.position())
                && converted.getDeltaMovement().equals(frameworkDrop.getDeltaMovement()), "FPSMatch throw physics survive conversion");
        original.makeFakeItem();
        helper.assertTrue(MatchDrops.copy(original, map) == null, "Visual /give pickup effects must not become real unlimited drops");
        map.getCapabilityMap().clear();
        helper.succeed();
    }
}
