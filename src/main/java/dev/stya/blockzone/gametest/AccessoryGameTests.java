package dev.stya.blockzone.gametest;

import com.mojang.authlib.GameProfile;
import dev.stya.blockzone.equipment.AccessoryCompat;
import dev.stya.blockzone.equipment.AccessoryItem;
import dev.stya.blockzone.equipment.StartingLoadout;
import dev.stya.blockzone.inventory.*;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;

@GameTestHolder("blockzone")
@PrefixGameTestTemplate(false)
public final class AccessoryGameTests {
    private static final ResourceLocation NATIVE = ResourceLocation.fromNamespaceAndPath("blockzone", "inventory_test_accessory");
    private static final ResourceLocation EXTERNAL = ResourceLocation.fromNamespaceAndPath("blockzone", "inventory_test_curio");
    public static class TestAccessory extends AccessoryItem {
        public TestAccessory() { super(new Item.Properties()); }
        @Override public void accessoryTick(Player player, int index, ItemStack stack) {
            stack.getOrCreateTag().putInt("Ticks", stack.getOrCreateTag().getInt("Ticks") + 1);
        }
        @Override public boolean canUnequip(Player player, int index, ItemStack stack) {
            return !stack.getOrCreateTag().getBoolean("Locked");
        }
    }
    private static final class CuriosTestFactory {
        static Item create() { return new ExternalCurio(); }
        static void tickAndValidate(Player player, ItemStack stack) {
            var context = new top.theillusivec4.curios.api.SlotContext(AccessoryCompat.SLOT, player, 0, false, true);
            check(top.theillusivec4.curios.api.CuriosApi.isStackValid(context, stack), "Native Curios validator recognizes accessory");
            check(!top.theillusivec4.curios.api.CuriosApi.isStackValid(context, new ItemStack(Items.STONE)), "Native Curios validator rejects ordinary item");
            top.theillusivec4.curios.api.CuriosApi.getCurio(stack).resolve().orElseThrow().curioTick(context);
            var other = new top.theillusivec4.curios.api.event.CurioEquipEvent(stack,
                    new top.theillusivec4.curios.api.SlotContext("ring", player, 0, false, true));
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(other);
            check(other.getResult() == net.minecraftforge.eventbus.api.Event.Result.DENY, "Other Curios slots cannot bypass the two-slot match limit");
        }
        static void checkDeathRule(Player player, ItemStack stack) {
            var handler = top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player).resolve().orElseThrow();
            var event = new top.theillusivec4.curios.api.event.DropRulesEvent(player, handler, player.damageSources().generic(), 0, false);
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(event);
            check(event.getOverrides().stream().anyMatch(rule -> rule.getA().test(stack)
                    && rule.getB() == top.theillusivec4.curios.api.type.capability.ICurio.DropRule.ALWAYS_KEEP),
                    "Curios must not also drop the match-owned accessory");
        }
        private static final class ExternalCurio extends Item implements top.theillusivec4.curios.api.type.capability.ICurioItem {
            ExternalCurio() { super(new Item.Properties().stacksTo(1)); }
        }
    }
    @Mod.EventBusSubscriber(modid = "blockzone", bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        @SubscribeEvent public static void items(RegisterEvent event) {
            if (!Boolean.getBoolean("forge.enableGameTest")) return;
            event.register(net.minecraft.core.registries.Registries.ITEM, helper -> {
                helper.register(NATIVE, new TestAccessory());
                helper.register(EXTERNAL, AccessoryCompat.loaded() ? CuriosTestFactory.create() : new Item(new Item.Properties()));
            });
        }
    }
    private static net.minecraftforge.common.util.FakePlayer player(GameTestHelper helper) {
        var player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "accessory_test"));
        player.getInventory().clearContent();
        return player;
    }
    private static ItemStack accessory() { return new ItemStack(ForgeRegistries.ITEMS.getValue(NATIVE)); }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    private static void rejects(Runnable action) {
        try { action.run(); } catch (IllegalArgumentException expected) { return; }
        throw new AssertionError("Expected configuration rejection");
    }

    @GameTest(template = "empty")
    public static void typeWhitelistCannotTurnOrdinaryItemsIntoEquipment(GameTestHelper helper) {
        var inventory = new PanelInventory(slot -> SlotRule.parse(List.of("*"), 64));
        check(!inventory.isItemValid(InventorySlot.ACCESSORY_1.index(), new ItemStack(Items.STONE)), "Ordinary items are not accessories");
        check(!inventory.isItemValid(InventorySlot.PRIMARY_WEAPON.index(), new ItemStack(Items.STONE)), "Ordinary items are not guns");
        check(!inventory.isItemValid(InventorySlot.HELMET.index(), new ItemStack(Items.DIAMOND_BOOTS)), "Wrong armor part rejected");
        check(inventory.isItemValid(InventorySlot.ACCESSORY_1.index(), accessory()), "Native accessory admitted");
        check(inventory.isItemValid(InventorySlot.BOOTS.index(), new ItemStack(Items.DIAMOND_BOOTS)), "Correct armor part admitted");
        rejects(() -> new InventoryRules(Map.of("accessory.1", new InventoryRules.Definition(List.of("minecraft:stone"), 1))).validateTypes());
        rejects(() -> new InventoryRules(Map.of("weapon.primary", new InventoryRules.Definition(List.of("minecraft:stone"), 1))).validateTypes());
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void fullStartingInventoryUsesActualArmorWeaponsAndReserves(GameTestHelper helper) {
        var player = player(helper);
        var rules = InventoryRules.defaults().withSlot(InventorySlot.MEDKIT, new InventoryRules.Definition(List.of("minecraft:apple"), 3))
                .withSlot(InventorySlot.AMMO_1, new InventoryRules.Definition(List.of("minecraft:paper"), 4)).compile();
        var entries = new java.util.ArrayList<>(StartingLoadout.defaults());
        entries.add(new StartingLoadout.Entry("accessory.1", NATIVE, 1, "{Identity:7}"));
        entries.add(new StartingLoadout.Entry("weapon.primary", ResourceLocation.parse("tacz:modern_kinetic_gun"), 1, "{GunId:\"tacz:ak47\"}"));
        entries.add(new StartingLoadout.Entry("item.medkit", ResourceLocation.parse("minecraft:apple"), 3, "{}"));
        entries.add(new StartingLoadout.Entry("ammo.1", ResourceLocation.parse("minecraft:paper"), 4, "{}"));
        var prepared = StartingLoadout.prepare(entries, rules::get);
        var data = PlayerPanelInventory.get(player).orElseThrow();
        data.begin("test", UUID.randomUUID(), rules::get);
        StartingLoadout.apply(player, prepared);
        check(data.getStack(InventorySlot.LEGGINGS) == player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.LEGS), "Leggings read native equipment");
        check(data.getStack(InventorySlot.BOOTS) == player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.FEET), "Boots read native equipment");
        check(!player.getInventory().getItem(0).isEmpty(), "Initial gun is usable in hotbar");
        check(player.getInventory().getItem(4).getCount() == 3, "Initial utility uses native hotbar");
        check(data.getStack(InventorySlot.AMMO_1).getCount() == 4, "Initial ammunition uses custom reserve");
        check(data.getStack(InventorySlot.ACCESSORY_1).getTag().getInt("Identity") == 7, "Accessory NBT retained");
        if (AccessoryCompat.loaded()) {
            var handler = AccessoryCompat.inventory(player).orElseThrow();
            check(handler.getStackInSlot(0) == data.getStack(InventorySlot.ACCESSORY_1), "Curios is the actual owner");
            check(data.storage().getStackInSlot(InventorySlot.ACCESSORY_1.index()).isEmpty(), "No duplicate in reserve storage");
        } else {
            data.tickAccessories();
            check(data.getStack(InventorySlot.ACCESSORY_1).getTag().getInt("Ticks") == 1, "Standalone accessory ticks");
        }
        data.end();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void invalidInitialInventoryIsRejectedBeforeMutation(GameTestHelper helper) {
        var before = StartingLoadout.defaults();
        for (var entry : List.of(
                new StartingLoadout.Entry("accessory.1", ResourceLocation.parse("minecraft:stone"), 1, "{}"),
                new StartingLoadout.Entry("equipment.legs", ResourceLocation.parse("blockzone:tactical_boots"), 1, "{}"),
                new StartingLoadout.Entry("weapon.primary", ResourceLocation.parse("minecraft:apple"), 1, "{}"),
                new StartingLoadout.Entry("item.armor_plates", ResourceLocation.parse("blockzone:armor_plate"), 17, "{}")))
            rejects(() -> StartingLoadout.prepare(List.of(entry)));
        var duplicates = new java.util.ArrayList<>(before);
        duplicates.add(new StartingLoadout.Entry("equipment.helmet", ResourceLocation.parse("blockzone:tactical_helmet"), 1, "{}"));
        rejects(() -> StartingLoadout.prepare(duplicates));
        var rules = InventoryRules.defaults().withSlot(InventorySlot.AMMO_1, new InventoryRules.Definition(List.of("minecraft:paper"), 4))
                .withSlot(InventorySlot.AMMO_2, new InventoryRules.Definition(List.of("minecraft:paper"), 4)).compile();
        rejects(() -> StartingLoadout.prepare(List.of(
                new StartingLoadout.Entry("ammo.1", ResourceLocation.parse("minecraft:paper"), 2, "{}"),
                new StartingLoadout.Entry("ammo.2", ResourceLocation.parse("minecraft:paper"), 2, "{}")), rules::get));
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void twoAccessorySlotsRespectLockAndOptionalApiAndRestoreOutsideItems(GameTestHelper helper) {
        var player = player(helper);
        var external = AccessoryCompat.inventory(player);
        var original = accessory(); original.getOrCreateTag().putString("Owner", "outside-match");
        external.ifPresent(handler -> handler.setStackInSlot(0, original.copy()));
        var data = PlayerPanelInventory.get(player).orElseThrow();
        data.begin("test", UUID.randomUUID(), InventoryRules.defaults().compile()::get);
        check(data.insert(accessory(), false).isEmpty(), "First accessory equips");
        var second = AccessoryCompat.loaded() ? new ItemStack(ForgeRegistries.ITEMS.getValue(EXTERNAL)) : accessory();
        check(data.insert(second, false).isEmpty(), "Second accessory equips");
        check(!data.insert(accessory(), false).isEmpty(), "Third accessory rejected");
        var locked = data.getStack(InventorySlot.ACCESSORY_1); locked.getOrCreateTag().putBoolean("Locked", true);
        check(!data.discard(InventorySlot.ACCESSORY_1, 2, data.round(), locked.copy()), "Curse/lock respected");
        locked.getOrCreateTag().putBoolean("Locked", false);
        check(data.discard(InventorySlot.ACCESSORY_1, 2, data.round(), locked.copy()), "Unlocked accessory can drop");
        data.end();
        external.ifPresent(handler -> check(ItemStack.isSameItemSameTags(original, handler.getStackInSlot(0)), "Outside accessories restored"));
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void legacyStorageIsRejectedAndCurrentStorageRoundTrips(GameTestHelper helper) {
        var data = new PanelInventory(slot -> SlotRule.parse(List.of("minecraft:paper"), 4));
        data.insertAmmo(new ItemStack(Items.PAPER, 3), false);
        for (int size : new int[] {17, 21}) {
            try {
                data.deserializeNBT(new net.minecraftforge.items.ItemStackHandler(size).serializeNBT());
                throw new AssertionError("Legacy storage accepted");
            } catch (IllegalArgumentException expected) { }
            check(data.getStackInSlot(InventorySlot.AMMO_1.index()).getCount() == 3, "Rejected load is atomic");
        }
        var restored = new PanelInventory(slot -> SlotRule.parse(List.of("minecraft:paper"), 4));
        restored.deserializeNBT(data.serializeNBT());
        check(restored.getSlots() == 17 && restored.getStackInSlot(InventorySlot.AMMO_1.index()).getCount() == 3, "Current schema round trips");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void accessoryDeathAndCloneNeverDuplicateMatchOrOutsideItems(GameTestHelper helper) {
        var player = player(helper);
        var original = accessory(); original.getOrCreateTag().putString("Owner", "outside");
        AccessoryCompat.inventory(player).ifPresent(handler -> handler.setStackInSlot(1, original.copy()));
        var data = PlayerPanelInventory.get(player).orElseThrow();
        data.begin("test", UUID.randomUUID(), InventoryRules.defaults().compile()::get);
        data.insert(accessory(), false);
        if (AccessoryCompat.loaded()) CuriosTestFactory.checkDeathRule(player, data.getStack(InventorySlot.ACCESSORY_1));
        var drops = new java.util.ArrayList<net.minecraft.world.entity.item.ItemEntity>();
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.entity.living.LivingDropsEvent(
                player, player.damageSources().generic(), drops, 0, false));
        check(drops.size() == 1 && drops.get(0).getItem().getCount() == 1, "Exactly one match accessory dropped");
        var clone = player(helper);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.entity.player.PlayerEvent.Clone(clone, player, true));
        var next = PlayerPanelInventory.get(clone).orElseThrow();
        next.restorePendingAccessories();
        check(!next.hasSession(), "Clone cannot resurrect match inventory");
        check(next.storage().getStackInSlot(InventorySlot.ACCESSORY_1.index()).isEmpty(), "No match item duplicated into storage");
        AccessoryCompat.inventory(clone).ifPresent(handler -> {
            check(handler.getStackInSlot(0).isEmpty(), "Match accessory not cloned back");
            check(ItemStack.isSameItemSameTags(original, handler.getStackInSlot(1)), "Outside accessory restored to clone");
        });
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void tickingNbtCanBeDiscardedButReplacedAccessoriesRejectOldClicks(GameTestHelper helper) {
        var player = player(helper);
        var data = PlayerPanelInventory.get(player).orElseThrow();
        data.begin("test", UUID.randomUUID(), InventoryRules.defaults().compile()::get);
        data.insert(accessory(), false);
        int revision = data.revision(InventorySlot.ACCESSORY_1);
        if (AccessoryCompat.loaded()) CuriosTestFactory.tickAndValidate(player, data.getStack(InventorySlot.ACCESSORY_1));
        data.tickAccessories();
        check(data.getStack(InventorySlot.ACCESSORY_1).getTag().getInt("Ticks") == 1, "Exactly one accessory tick through the correct owner");
        check(data.revision(InventorySlot.ACCESSORY_1) == revision, "Ticking NBT does not change item identity");
        check(data.discard(InventorySlot.ACCESSORY_1, 2, data.round(), revision), "Ticking accessory remains discardable");
        data.insert(accessory(), false);
        int previous = data.revision(InventorySlot.ACCESSORY_1);
        data.equip(InventorySlot.ACCESSORY_1, accessory());
        check(!data.discard(InventorySlot.ACCESSORY_1, 2, data.round(), previous), "A replaced identical item invalidates the old click");
        data.end();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void nonDeathForgeCloneMovesActualAccessoriesAndTheirOriginalSnapshot(GameTestHelper helper) {
        var player = player(helper);
        var outside = accessory(); outside.getOrCreateTag().putString("Owner", "outside-clone");
        AccessoryCompat.inventory(player).ifPresent(handler -> handler.setStackInSlot(1, outside.copy()));
        var data = PlayerPanelInventory.get(player).orElseThrow();
        data.begin("test", UUID.randomUUID(), InventoryRules.defaults().compile()::get);
        var match = accessory(); match.getOrCreateTag().putString("Owner", "match-clone");
        data.insert(match, false);
        var clone = player(helper);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.entity.player.PlayerEvent.Clone(clone, player, false));
        var next = PlayerPanelInventory.get(clone).orElseThrow();
        check(!data.hasSession() && next.hasSession() && !next.active(), "Only the clone retains a session awaiting validation");
        check(ItemStack.isSameItemSameTags(match, next.getStack(InventorySlot.ACCESSORY_1)), "Actual accessory transferred to clone");
        next.end();
        AccessoryCompat.inventory(clone).ifPresent(handler -> {
            check(handler.getStackInSlot(0).isEmpty(), "Match equipment removed when clone session ends");
            check(ItemStack.isSameItemSameTags(outside, handler.getStackInSlot(1)), "Original outside equipment follows clone");
        });
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void savedInitialInventoryCanLoadBeforeItsSlotRules(GameTestHelper helper) {
        var loadout = new dev.stya.blockzone.map.battlezone.capability.BattlezoneLoadoutCapability(null);
        loadout.decode(com.google.gson.JsonParser.parseString("[{\"slot\":\"item.medkit\",\"item\":\"minecraft:apple\",\"count\":3}]"));
        check(loadout.read().size() == 1, "Saved loadout restores without assuming its inventory capability is decoded first");
        var rules = new dev.stya.blockzone.map.battlezone.capability.BattlezoneInventoryCapability(null);
        rules.decode(com.google.gson.JsonParser.parseString("{\"item.medkit\":{\"allowed\":[\"minecraft:apple\"],\"limit\":3}}"));
        check(StartingLoadout.prepare(loadout.read(), rules.read().compile()::get).size() == 1, "Combined restored configuration validates");
        rejects(() -> StartingLoadout.prepare(loadout.read()));
        helper.succeed();
    }
}
