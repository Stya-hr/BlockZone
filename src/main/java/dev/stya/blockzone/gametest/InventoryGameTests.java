package dev.stya.blockzone.gametest;

import dev.stya.blockzone.inventory.*;

import java.util.List;
import java.util.Map;
import dev.stya.blockzone.map.battlezone.capability.BattlezoneInventoryCapability;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("blockzone")
@PrefixGameTestTemplate(false)
public final class InventoryGameTests {

    @GameTest(template = "empty")
    public static void ammoUsesOneDynamicSlotWithoutOverflowAndReleasesWhenEmpty(GameTestHelper helper) {
        var inventory = new PanelInventory(slot -> SlotRule.parse(List.of("*"), 4));
        int first = InventorySlot.AMMO_1.index(), second = InventorySlot.AMMO_2.index();
        assertEquals(2, inventory.insertAmmo(new ItemStack(Items.PAPER, 6), true).getCount());
        assertTrue(inventory.getStackInSlot(first).isEmpty());
        assertEquals(2, inventory.insertAmmo(new ItemStack(Items.PAPER, 6), false).getCount());
        assertEquals(4, inventory.getStackInSlot(first).getCount());
        assertTrue(inventory.getStackInSlot(second).isEmpty());
        assertEquals(3, inventory.insertItem(second, new ItemStack(Items.PAPER, 3), false).getCount());
        assertThrows(IllegalArgumentException.class, () -> inventory.setStackInSlot(second, new ItemStack(Items.PAPER)));
        assertTrue(inventory.insertAmmo(new ItemStack(Items.STICK, 2), false).isEmpty());
        assertEquals(2, inventory.getStackInSlot(second).getCount());
        inventory.extractItem(first, 4, false);
        assertTrue(inventory.insertAmmo(new ItemStack(Items.STONE), false).isEmpty());
        assertTrue(inventory.getStackInSlot(first).is(Items.STONE));
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void ammoRoutingRespectsSlotRulesAndPerItemLimits(GameTestHelper helper) {
        var inventory = new PanelInventory(slot -> slot == InventorySlot.AMMO_2
                ? SlotRule.parse(List.of("minecraft:paper"), 8, Map.of("minecraft:paper", 3))
                : SlotRule.parse(List.of("minecraft:stick"), 4));
        assertEquals(2, inventory.insertAmmo(new ItemStack(Items.PAPER, 5), false).getCount());
        assertTrue(inventory.getStackInSlot(InventorySlot.AMMO_1.index()).isEmpty());
        assertEquals(3, inventory.getStackInSlot(InventorySlot.AMMO_2.index()).getCount());
        assertEquals(1, inventory.insertAmmo(new ItemStack(Items.PAPER), false).getCount());
        assertEquals(1, inventory.insertAmmo(new ItemStack(Items.STONE), false).getCount());
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void taczAmmoIdsAndUnidictPhysicalItemsDefineTypesRatherThanFullNbt(GameTestHelper helper) throws Exception {
        var inventory = new PanelInventory(slot -> SlotRule.parse(List.of("*"), 4));
        var light = com.tacz.guns.api.item.builder.AmmoItemBuilder.create()
                .setId(ResourceLocation.fromNamespaceAndPath("tacz", "9mm")).setCount(4).build();
        var heavy = com.tacz.guns.api.item.builder.AmmoItemBuilder.create()
                .setId(ResourceLocation.fromNamespaceAndPath("tacz", "762x39")).setCount(2).build();
        assertTrue(inventory.insertAmmo(light, false).isEmpty());
        assertTrue(inventory.insertAmmo(heavy, false).isEmpty());
        var decorated = light.copy();
        decorated.getOrCreateTag().putString("display_note", "same ammo, different NBT");
        assertEquals(4, inventory.insertAmmo(decorated, false).getCount());
        assertTrue(inventory.getStackInSlot(InventorySlot.AMMO_3.index()).isEmpty());
        // Unidict supports ordinary items as ammo. Extra NBT must not create another type.
        assertTrue(inventory.insertAmmo(new ItemStack(Items.PAPER), false).isEmpty());
        var paper = new ItemStack(Items.PAPER);
        paper.setTag(TagParser.parseTag("{custom:1}"));
        assertEquals(1, inventory.insertAmmo(paper, false).getCount());
        assertTrue(inventory.getStackInSlot(InventorySlot.AMMO_4.index()).isEmpty());
        if (net.minecraftforge.fml.ModList.get().isLoaded("tacz_unidict")) verifyUnidictRuntimeAmmo();
        helper.succeed();
    }

    private static void verifyUnidictRuntimeAmmo() throws Exception {
        // Optional integration test: no production or test compile dependency on Unidict.
        var manager = Class.forName("com.scarasol.tud.manager.AmmoManager");
        var gun = com.tacz.guns.api.item.builder.GunItemBuilder.create()
                .setId(ResourceLocation.fromNamespaceAndPath("tacz", "ak47")).build();
        var ammo = (ItemStack) manager.getMethod("getGunAmmo", ItemStack.class).invoke(null, gun);
        assertFalse(ammo.isEmpty());
        ammo.setCount(4);
        var inventory = new PanelInventory(slot -> SlotRule.parse(List.of("*"), 4));
        assertTrue(inventory.insertAmmo(ammo, false).isEmpty());
        assertEquals(4, inventory.insertAmmo(ammo, false).getCount());
        var gunItem = (com.tacz.guns.api.item.gun.AbstractGunItem) gun.getItem();
        assertEquals(2, gunItem.findAndExtractInventoryAmmo(inventory, gun, 2));
        assertEquals(2, inventory.getStackInSlot(InventorySlot.AMMO_1.index()).getCount());
        var ordinary = (ItemStack) manager.getMethod("getAmmoItemStack", net.minecraft.util.Tuple.class)
                .invoke(null, new net.minecraft.util.Tuple<>(ResourceLocation.fromNamespaceAndPath("minecraft", "paper"), true));
        assertTrue(ordinary.is(Items.PAPER));
        assertTrue(inventory.insertAmmo(ordinary, false).isEmpty());
        assertEquals(4, inventory.insertAmmo(new ItemStack(Items.PAPER, 7), false).getCount());
        assertTrue(inventory.getStackInSlot(InventorySlot.AMMO_3.index()).isEmpty());
    }

    @GameTest(template = "empty")
    public static void duplicateSavedAmmoIsRejectedAtomicallyWithoutDeletingStoredItems(GameTestHelper helper) {
        var inventory = new PanelInventory(slot -> SlotRule.parse(List.of("*"), 4));
        inventory.insertAmmo(new ItemStack(Items.STICK, 2), false);
        var invalid = new net.minecraftforge.items.ItemStackHandler(17);
        invalid.setStackInSlot(InventorySlot.AMMO_1.index(), new ItemStack(Items.PAPER));
        invalid.setStackInSlot(InventorySlot.AMMO_2.index(), new ItemStack(Items.PAPER));
        var invalidNbt = invalid.serializeNBT();
        invalidNbt.putInt("Schema", 2);
        assertThrows(IllegalArgumentException.class, () -> inventory.deserializeNBT(invalidNbt));
        assertTrue(inventory.getStackInSlot(InventorySlot.AMMO_1.index()).is(Items.STICK));
        assertEquals(2, inventory.getStackInSlot(InventorySlot.AMMO_1.index()).getCount());
        var restored = new PanelInventory(slot -> SlotRule.parse(List.of("*"), 4));
        restored.deserializeNBT(inventory.serializeNBT());
        assertEquals(1, restored.insertItem(InventorySlot.AMMO_2.index(), new ItemStack(Items.STICK), false).getCount());
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void differentItemsHaveIndependentLimitsAndNativeStackCaps(GameTestHelper helper) {
        var rule = SlotRule.parse(List.of("minecraft:golden_apple", "minecraft:paper", "minecraft:stick", "minecraft:iron_sword"),
                4, Map.of("minecraft:golden_apple", 8, "minecraft:paper", 16, "minecraft:iron_sword", 2));
        var inventory = new PanelInventory(slot -> rule);
        assertEquals(2, inventory.insertItem(8, new ItemStack(Items.GOLDEN_APPLE, 10), true).getCount());
        assertTrue(inventory.getStackInSlot(8).isEmpty());
        assertEquals(2, inventory.insertItem(8, new ItemStack(Items.GOLDEN_APPLE, 10), false).getCount());
        assertEquals(8, inventory.getStackInSlot(8).getCount());
        assertEquals(4, inventory.insertItem(9, new ItemStack(Items.PAPER, 20), false).getCount());
        assertEquals(16, inventory.getStackInSlot(9).getCount());
        assertEquals(2, inventory.insertItem(10, new ItemStack(Items.STICK, 6), false).getCount());
        assertEquals(1, inventory.insertItem(11, new ItemStack(Items.IRON_SWORD, 2), false).getCount());
        assertThrows(IllegalArgumentException.class, () -> inventory.setStackInSlot(8, new ItemStack(Items.GOLDEN_APPLE, 9)));
        assertThrows(IllegalArgumentException.class, () -> inventory.setStackInSlot(11, new ItemStack(Items.IRON_SWORD, 2)));
        inventory.extractItem(8, 8, false);
        inventory.setStackInSlot(8, new ItemStack(Items.PAPER, 16));
        assertEquals(16, inventory.getStackInSlot(8).getCount());
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void overlappingNbtRulesUseStrictestOverrideAndTighteningPreservesItems(GameTestHelper helper) throws Exception {
        var definition = new InventoryRules.Definition(List.of("minecraft:paper", "minecraft:paper{medical:1}"), 4,
                Map.of("minecraft:paper", 16, "minecraft:paper{medical:1}", 2));
        var cap = new BattlezoneInventoryCapability(null);
        cap.write(cap.read().withSlot(InventorySlot.TACTICAL_2, definition));
        var inventory = cap.createInventory();
        var medical = new ItemStack(Items.PAPER, 5);
        medical.setTag(TagParser.parseTag("{medical:1,extra:3}"));
        int slot = InventorySlot.TACTICAL_2.index();
        assertEquals(3, inventory.insertItem(slot, medical, false).getCount());
        assertEquals(2, inventory.getStackInSlot(slot).getCount());
        var saved = inventory.serializeNBT();
        cap.write(cap.read().withSlot(InventorySlot.TACTICAL_2,
                new InventoryRules.Definition(definition.allowed(), 4, Map.of("minecraft:paper", 1))));
        inventory.deserializeNBT(saved);
        assertEquals(2, inventory.getStackInSlot(slot).getCount());
        assertEquals(5, inventory.insertItem(slot, medical, false).getCount());
        assertEquals(2, inventory.extractItem(slot, 2, false).getCount());
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void vanillaPredicateMatchesItemAndPartialNbtButRejectsUnknownIds(GameTestHelper helper) throws Exception {
        var selector = ItemSelector.parse("minecraft:stick{custom:{kind:\"medical\"}}");
        var stack = new ItemStack(Items.STICK);
        stack.setTag(TagParser.parseTag("{custom:{kind:\"medical\",charge:4},other:1}"));
        assertTrue(selector.matches(stack));
        assertFalse(selector.matches(new ItemStack(Items.STICK)));
        assertFalse(selector.matches(new ItemStack(Items.STONE)));
        assertFalse(ItemSelector.parse("missing_mod:missing_item").matches(stack));
        assertFalse(ItemSelector.parse("*").matches(ItemStack.EMPTY));
        assertFalse(ItemSelector.parse("@gun:*").matches(stack));
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void nativeTagPredicateSeesReboundTagsWithoutRecompilingRules(GameTestHelper helper) {
        var tag = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("blockzone", "test_allowed"));
        var previous = new java.util.HashMap<TagKey<Item>, List<net.minecraft.core.Holder<Item>>>();
        BuiltInRegistries.ITEM.getTags().forEach(pair -> previous.put(pair.getFirst(), pair.getSecond().stream().toList()));
        var selector = ItemSelector.parse("#blockzone:test_allowed");
        try {
            BuiltInRegistries.ITEM.bindTags(Map.of(tag, List.of(BuiltInRegistries.ITEM.wrapAsHolder(Items.STICK))));
            assertTrue(selector.matches(new ItemStack(Items.STICK)));
            BuiltInRegistries.ITEM.bindTags(Map.of(tag, List.of(BuiltInRegistries.ITEM.wrapAsHolder(Items.STONE))));
            assertFalse(selector.matches(new ItemStack(Items.STICK)));
            assertTrue(selector.matches(new ItemStack(Items.STONE)));
        } finally { BuiltInRegistries.ITEM.bindTags(previous); }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void insertionAndDirectReplacementEnforceRulesAndStackLimits(GameTestHelper helper) {
        int slot = InventorySlot.TACTICAL_2.index();
        var inventory = new PanelInventory(target -> SlotRule.parse(List.of("minecraft:stick"), 4));
        assertEquals(2, inventory.insertItem(slot, new ItemStack(Items.STICK, 6), true).getCount());
        assertTrue(inventory.getStackInSlot(slot).isEmpty());
        assertEquals(2, inventory.insertItem(slot, new ItemStack(Items.STICK, 6), false).getCount());
        assertEquals(4, inventory.getStackInSlot(slot).getCount());
        assertEquals(1, inventory.insertItem(slot, new ItemStack(Items.STONE), false).getCount());
        assertThrows(IllegalArgumentException.class, () -> inventory.setStackInSlot(slot, new ItemStack(Items.STONE)));
        assertThrows(IllegalArgumentException.class, () -> inventory.setStackInSlot(slot, new ItemStack(Items.STICK, 5)));
        assertEquals(4, inventory.getStackInSlot(slot).getCount());
        var singleItem = new PanelInventory(target -> SlotRule.parse(List.of("*"), 64));
        assertEquals(1, singleItem.insertItem(slot, new ItemStack(Items.IRON_SWORD, 2), false).getCount());
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void tightenedRulesPreserveStoredItemsAndStillAllowExtraction(GameTestHelper helper) {
        var cap = new BattlezoneInventoryCapability(null);
        cap.write(new InventoryRules(Map.of("item.tactical.2", new InventoryRules.Definition(List.of("minecraft:stick"), 8))));
        var inventory = cap.createInventory();
        int slot = InventorySlot.TACTICAL_2.index();
        inventory.insertItem(slot, new ItemStack(Items.STICK, 5), false);
        var saved = inventory.serializeNBT();
        cap.write(new InventoryRules(Map.of("item.tactical.2", new InventoryRules.Definition(List.of(), 1))));
        var restored = cap.createInventory();
        restored.deserializeNBT(saved);
        assertEquals(InventorySlot.ALL.size(), restored.getSlots());
        assertEquals(5, restored.getStackInSlot(slot).getCount());
        assertEquals(1, restored.insertItem(slot, new ItemStack(Items.STICK), false).getCount());
        assertEquals(5, restored.extractItem(slot, 5, false).getCount());
        assertTrue(restored.getStackInSlot(slot).isEmpty());
        saved.putInt("Size", 99);
        assertThrows(IllegalArgumentException.class, () -> restored.deserializeNBT(saved));
        helper.succeed();
    }

    private static void assertTrue(boolean condition) {
        if (!condition) throw new AssertionError("Expected true");
    }
    private static void assertFalse(boolean condition) { assertTrue(!condition); }
    private static void assertEquals(int expected, int actual) {
        if (expected != actual) throw new AssertionError("Expected " + expected + " but got " + actual);
    }
    private static void assertThrows(Class<? extends Throwable> type, Runnable action) {
        try { action.run(); }
        catch (Throwable failure) {
            if (type.isInstance(failure)) return;
            throw new AssertionError("Unexpected failure", failure);
        }
        throw new AssertionError("Expected " + type.getSimpleName());
    }
}
