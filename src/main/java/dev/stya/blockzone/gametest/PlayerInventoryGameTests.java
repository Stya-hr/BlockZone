package dev.stya.blockzone.gametest;

import com.mojang.authlib.GameProfile;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.builder.AmmoItemBuilder;
import com.tacz.guns.api.item.builder.GunItemBuilder;
import com.tacz.guns.api.item.gun.AbstractGunItem;
import com.tacz.guns.item.ModernKineticGunScriptAPI;
import dev.stya.blockzone.inventory.*;
import java.util.List;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("blockzone")
@PrefixGameTestTemplate(false)
public final class PlayerInventoryGameTests {
    private static FakePlayer player(GameTestHelper helper) {
        var player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "inventory_test"));
        player.setGameMode(net.minecraft.world.level.GameType.ADVENTURE);
        player.getInventory().clearContent();
        var data = PlayerPanelInventory.get(player).orElseThrow();
        data.begin("gametest", UUID.randomUUID(), slot -> InventorySlot.WEAPONS.contains(slot)
                ? SlotRule.parse(List.of("@gun:*"), 1) : SlotRule.parse(List.of("tacz:ammo", "minecraft:paper"), 4));
        return player;
    }

    private static ItemStack gun() {
        return GunItemBuilder.create().setId(ResourceLocation.fromNamespaceAndPath("tacz", "ak47")).build();
    }

    private static ItemStack ammo(ItemStack gun) throws Exception {
        if (net.minecraftforge.fml.ModList.get().isLoaded("tacz_unidict"))
            return (ItemStack) Class.forName("com.scarasol.tud.manager.AmmoManager")
                    .getMethod("getGunAmmo", ItemStack.class).invoke(null, gun);
        var id = ((AbstractGunItem) gun.getItem()).getGunId(gun);
        return AmmoItemBuilder.create().setId(TimelessAPI.getCommonGunIndex(id).orElseThrow().getGunData().getAmmoId()).build();
    }

    private static ItemEntity drop(GameTestHelper helper, ItemStack stack) {
        var drop = new ItemEntity(helper.getLevel(), 0, 2, 0, stack.copy());
        drop.setNoPickUpDelay();
        return drop;
    }

    @GameTest(template = "empty")
    public static void worldPickupUsesRealHotbarWeaponsAndRejectsThirdGun(GameTestHelper helper) {
        var player = player(helper);
        var weapon = gun();
        weapon.getOrCreateTag().putString("CustomIdentity", "keep-full-nbt");
        var first = drop(helper, weapon);
        first.playerTouch(player);
        check(first.isRemoved(), "First gun should be fully picked up");
        check(ItemStack.isSameItemSameTags(weapon, player.getInventory().getItem(0)), "Gun NBT must survive pickup");
        var second = drop(helper, gun()); second.playerTouch(player);
        check(second.isRemoved(), "Second gun should enter second hotbar slot");
        var third = drop(helper, gun()); third.playerTouch(player);
        check(!third.isRemoved() && third.getItem().getCount() == 1, "Third gun must remain on ground");
        var data = PlayerPanelInventory.get(player).orElseThrow();
        check(data.storage().getStackInSlot(InventorySlot.PRIMARY_WEAPON.index()).isEmpty(), "No duplicate gun in custom storage");
        player.getInventory().getItem(0).getOrCreateTag().putInt("LiveAmmo", 3);
        check(data.snapshot().get(InventorySlot.PRIMARY_WEAPON.index()).getTag().getInt("LiveAmmo") == 3,
                "Panel must read the actual held weapon");
        data.end();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void partialPickupHonorsExclusiveSlotsDelayAndOwnership(GameTestHelper helper) {
        var player = player(helper);
        var delayed = drop(helper, new ItemStack(Items.PAPER)); delayed.setPickUpDelay(20); delayed.playerTouch(player);
        check(!delayed.isRemoved(), "Pickup delay must remain effective");
        var owned = drop(helper, new ItemStack(Items.PAPER)); owned.setTarget(UUID.randomUUID()); owned.playerTouch(player);
        check(!owned.isRemoved(), "Other player's item must not be picked up");
        var drop = drop(helper, new ItemStack(Items.PAPER, 7)); drop.playerTouch(player);
        var data = PlayerPanelInventory.get(player).orElseThrow();
        check(drop.getItem().getCount() == 3, "Partial pickup must leave exact remainder");
        check(data.storage().getStackInSlot(InventorySlot.AMMO_1.index()).getCount() == 4, "Reserve should hold accepted amount");
        drop.playerTouch(player);
        check(drop.getItem().getCount() == 3 && data.storage().getStackInSlot(InventorySlot.AMMO_2.index()).isEmpty(),
                "Full type must not spill into another slot");
        check(player.getInventory().countItem(Items.PAPER) == 0, "Reserve must not be duplicated in vanilla inventory");
        data.end();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void reloadChecksAndScriptConsumptionUseOnlyPlayerReserves(GameTestHelper helper) throws Exception {
        var player = player(helper);
        var weapon = gun();
        var ammo = ammo(weapon); ammo.setCount(4);
        var gunItem = (AbstractGunItem) weapon.getItem();
        var data = PlayerPanelInventory.get(player).orElseThrow();
        player.getInventory().setItem(0, weapon);
        player.getInventory().setItem(9, ammo.copy());
        check(!gunItem.canReload(player, weapon), "Vanilla reserve must not bypass the match's ammunition slots");
        player.getInventory().setItem(9, ItemStack.EMPTY);
        drop(helper, ammo).playerTouch(player);
        check(gunItem.canReload(player, weapon), "Picked up custom reserve should allow reload");
        var script = new ModernKineticGunScriptAPI();
        script.setShooter(player); script.setItemStack(weapon);
        check(script.consumeAmmoFromPlayer(3) == 3, "TaCZ reload script must consume requested reserve");
        check(data.storage().getStackInSlot(InventorySlot.AMMO_1.index()).getCount() == 1, "Exactly three rounds consumed");
        check(script.consumeAmmoFromPlayer(3) == 1, "Reload must not invent missing rounds");
        check(!gunItem.canReload(player, weapon), "Empty reserve should prevent reload");
        data.end();
        player.getInventory().setItem(9, ammo);
        check(gunItem.canReload(player, weapon), "Outside match native inventory behavior must be restored");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void normalizationAndSerializationKeepOneSourceOfTruth(GameTestHelper helper) {
        var player = player(helper);
        var data = PlayerPanelInventory.get(player).orElseThrow();
        player.getInventory().setItem(0, new ItemStack(Items.STONE));
        player.getInventory().setItem(10, gun());
        player.getInventory().setItem(11, new ItemStack(Items.PAPER, 3));
        data.normalize();
        check(com.ptcrys.fpsmatch.compat.gun.GunCompatManager.isGun(player.getInventory().getItem(0)), "Loadout gun must move to real weapon slot");
        check(player.getInventory().getItem(10).isEmpty() && player.getInventory().getItem(11).isEmpty(), "Migration must clear old locations");
        check(player.getInventory().countItem(Items.STONE) == 1, "Ordinary starting equipment must survive relocation");
        var before = data.save();
        var oldView = data.ammunition().orElseThrow(IllegalStateException::new);
        data.load(before, true);
        check(oldView.getStackInSlot(0).getCount() == 3, "Cached reserve view must follow restored storage");
        data.load(before, false);
        check(!data.active() && data.hasSession(), "Disk state requires match/round validation before use");
        data.end();
        check(oldView.getStackInSlot(0).isEmpty(), "Ending session must clear reserve view");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void discardValidatesIdentityAndRoundAndClampsQuantities(GameTestHelper helper) {
        var player = player(helper);
        var data = PlayerPanelInventory.get(player).orElseThrow();
        data.begin("gametest", UUID.randomUUID(), slot -> SlotRule.parse(List.of("minecraft:paper"), 64));
        data.insert(new ItemStack(Items.PAPER, 14), false);
        var expected = data.getStack(InventorySlot.AMMO_1).copy();
        check(!data.discard(InventorySlot.AMMO_1, 0, UUID.randomUUID(), expected), "Old rounds must not discard items");
        check(!data.discard(InventorySlot.AMMO_1, 0, data.round(), new ItemStack(Items.STONE)), "Changed items must not be discarded");
        check(!data.discard(InventorySlot.AMMO_1, 3, data.round(), expected), "Unknown actions must be rejected");
        check(data.discard(InventorySlot.AMMO_1, 0, data.round(), expected), "Single discard should succeed");
        check(data.getStack(InventorySlot.AMMO_1).getCount() == 13, "Single discard removes one");
        check(data.discard(InventorySlot.AMMO_1, 1, data.round(), expected), "Bulk discard should succeed");
        check(data.getStack(InventorySlot.AMMO_1).getCount() == 3, "Bulk discard removes ten");
        check(data.discard(InventorySlot.AMMO_1, 2, data.round(), expected), "All discard should succeed");
        check(data.getStack(InventorySlot.AMMO_1).isEmpty(), "All discard empties slot");
        check(!data.discard(InventorySlot.AMMO_1, 2, data.round(), expected), "Replay cannot duplicate items");
        data.insert(new ItemStack(Items.PAPER, 2), false);
        check(data.discard(InventorySlot.AMMO_1, 1, data.round(), expected), "Bulk discard clamps to remainder");
        check(data.getStack(InventorySlot.AMMO_1).isEmpty(), "Small stacks cannot become negative");
        data.end();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void utilityPickupUsesNativeHotbarAndSynchronizesLimits(GameTestHelper helper) {
        var player = player(helper);
        var data = PlayerPanelInventory.get(player).orElseThrow();
        data.begin("gametest", UUID.randomUUID(), slot -> slot == InventorySlot.MEDKIT
                ? SlotRule.parse(List.of("minecraft:apple"), 3) : SlotRule.parse(List.of(), 1));
        var remainder = data.insert(new ItemStack(Items.APPLE, 5), false);
        check(remainder.getCount() == 2, "Utility pickup must honor configured limit");
        check(player.getInventory().getItem(4).getCount() == 3, "Utility must occupy actual usable hotbar slot");
        check(data.storage().getStackInSlot(InventorySlot.MEDKIT.index()).isEmpty(), "Utility cannot have a duplicate in storage");
        check(data.save().getIntArray("Limits")[InventorySlot.MEDKIT.index()] == 3, "UI must receive configured limit");
        var expected = data.getStack(InventorySlot.MEDKIT).copy();
        check(data.discard(InventorySlot.MEDKIT, 0, data.round(), expected), "Native utility should be discardable");
        check(player.getInventory().getItem(4).getCount() == 2, "Discard must mutate actual hotbar source");
        data.end();
        helper.succeed();
    }

    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }

    @GameTest(template = "empty")
    public static void actualDeathDropsReservesOnceAndCannotCloneThemBack(GameTestHelper helper) {
        var player = player(helper);
        var data = PlayerPanelInventory.get(player).orElseThrow();
        data.insert(new ItemStack(Items.PAPER, 4), false);
        var drops = new java.util.ArrayList<ItemEntity>();
        var event = new net.minecraftforge.event.entity.living.LivingDropsEvent(player, player.damageSources().generic(), drops, 0, false);
        PlayerInventoryEvents.actualDeath(event);
        PlayerInventoryEvents.actualDeath(event);
        check(drops.size() == 1 && drops.get(0).getItem().getCount() == 4, "Death must drop reserves exactly once");
        check(!data.hasSession() && data.storage().getStackInSlot(InventorySlot.AMMO_1.index()).isEmpty(), "Dropped reserves must leave storage");
        var clone = player(helper);
        PlayerInventoryEvents.clonePlayer(new net.minecraftforge.event.entity.player.PlayerEvent.Clone(clone, player, true));
        var cloned = PlayerPanelInventory.get(clone).orElseThrow();
        check(!cloned.hasSession() && cloned.storage().getStackInSlot(InventorySlot.AMMO_1.index()).isEmpty(), "Respawn must not recreate dropped reserves");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void nonDeathCloneMovesStoredReservesAndRequiresSessionValidation(GameTestHelper helper) {
        var original = player(helper);
        var data = PlayerPanelInventory.get(original).orElseThrow();
        data.insert(new ItemStack(Items.PAPER, 3), false);
        var clone = player(helper);
        PlayerInventoryEvents.clonePlayer(new net.minecraftforge.event.entity.player.PlayerEvent.Clone(clone, original, false));
        var cloned = PlayerPanelInventory.get(clone).orElseThrow();
        check(cloned.hasSession() && !cloned.active(), "Clone must validate map/round before use");
        check(cloned.storage().getStackInSlot(InventorySlot.AMMO_1.index()).getCount() == 3, "Clone must retain exact reserve count");
        check(!data.hasSession(), "Old player must no longer own the reserves");
        cloned.end();
        helper.succeed();
    }
}
