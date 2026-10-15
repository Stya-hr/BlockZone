package dev.stya.blockzone.gametest;

import com.mojang.authlib.GameProfile;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.builder.AmmoItemBuilder;
import com.tacz.guns.api.item.builder.GunItemBuilder;
import dev.stya.blockzone.equipment.WeaponAmmo;
import dev.stya.blockzone.inventory.*;
import java.util.List;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("blockzone")
@PrefixGameTestTemplate(false)
public final class WeaponHudGameTests {
    @GameTest(template = "empty")
    public static void weaponReadoutUsesReloadStorageAndDoesNotMutateTheGun(GameTestHelper helper) throws Exception {
        var player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "weapon_hud"));
        player.setGameMode(net.minecraft.world.level.GameType.ADVENTURE);
        player.getInventory().clearContent();
        var inventory = PlayerPanelInventory.get(player).orElseThrow();
        inventory.begin("hud_test", UUID.randomUUID(), slot -> SlotRule.parse(List.of("*"), 64));
        var stack = GunItemBuilder.create().setId(ResourceLocation.fromNamespaceAndPath("tacz", "ak47")).build();
        var gun = IGun.getIGunOrNull(stack);
        gun.setCurrentAmmoCount(stack, 19);
        gun.setBulletInBarrel(stack, true);
        var ammo = AmmoItemBuilder.create().setId(TimelessAPI.getCommonGunIndex(gun.getGunId(stack)).orElseThrow().getGunData().getAmmoId()).build();
        if (net.minecraftforge.fml.ModList.get().isLoaded("tacz_unidict")) ammo = (ItemStack)Class.forName("com.scarasol.tud.manager.AmmoManager")
                .getMethod("getGunAmmo", ItemStack.class).invoke(null, stack);
        ammo.setCount(47);
        inventory.storage().setStackInSlot(InventorySlot.AMMO_1.index(), ammo);
        player.getInventory().setItem(9, ammo.copy()); // Not a reload source during a match.
        var before = stack.save(new net.minecraft.nbt.CompoundTag());
        var view = WeaponAmmo.read(player, inventory, stack);
        helper.assertTrue(view.loaded() == 20 && view.reserve() == 47, "Count chamber plus magazine and only the actual reserve handler");
        helper.assertTrue(before.equals(stack.save(new net.minecraft.nbt.CompoundTag())), "Rendering must not write ammo into the gun");
        inventory.storage().extractItem(InventorySlot.AMMO_1.index(), 7, false);
        helper.assertTrue(WeaponAmmo.read(player, inventory, stack).reserve() == 40, "Reflect consumption without cached stale totals");
        helper.assertTrue(WeaponAmmo.read(player, inventory, ItemStack.EMPTY) == null, "Empty weapons have no ammo readout");
        inventory.end();
        helper.succeed();
    }
}
