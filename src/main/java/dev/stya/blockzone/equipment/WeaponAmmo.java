package dev.stya.blockzone.equipment;

import com.ptcrys.fpsmatch.compat.gun.GunCompatManager;
import dev.stya.blockzone.inventory.InventorySlot;
import dev.stya.blockzone.inventory.PlayerPanelInventory;
import java.lang.reflect.Method;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;

/** Read-only ammunition view of the same reserve storage used by the reload bridge. */
public final class WeaponAmmo {
    public record View(int loaded, int reserve, boolean unlimited, String fireMode) {}
    public static View read(Player player, PlayerPanelInventory inventory, ItemStack stack) {
        if (!GunCompatManager.isGun(stack)) return null;
        if (ModList.get().isLoaded("tacz")) {
            var view = Tacz.read(player, inventory, stack);
            if (view != null) return view;
        }
        var provider = GunCompatManager.findProvider(stack);
        return new View(Math.max(0, provider.getCurrentAmmo(stack)), Math.max(0, provider.getDummyAmmo(stack)), false, "");
    }

    private static final class Tacz {
        static View read(Player player, PlayerPanelInventory inventory, ItemStack stack) {
            var gun = com.tacz.guns.api.item.IGun.getIGunOrNull(stack);
            if (gun == null) return null;
            var data = com.tacz.guns.api.TimelessAPI.getCommonGunIndex(gun.getGunId(stack))
                    .map(com.tacz.guns.resource.index.CommonGunIndex::getGunData).orElse(null);
            boolean unlimited = !com.tacz.guns.api.entity.IGunOperator.fromLivingEntity(player).needCheckAmmo();
            long reserve = 0;
            if (gun.useDummyAmmo(stack)) reserve = Math.max(0, gun.getDummyAmmoAmount(stack));
            else for (var slot : InventorySlot.AMMUNITION) {
                var ammo = inventory.getStack(slot);
                if (ammo.isEmpty()) continue;
                if (ammo.getItem() instanceof com.tacz.guns.api.item.IAmmoBox box) {
                    if (box.isAllTypeCreative(ammo) || box.isAmmoBoxOfGun(stack, ammo) && box.isCreative(ammo)) unlimited = true;
                    else if (box.isAmmoBoxOfGun(stack, ammo)) reserve += Math.max(0, box.getAmmoCount(ammo));
                } else if (matches(stack, ammo)) reserve += ammo.getCount();
            }
            int remaining = (int)Math.min(Integer.MAX_VALUE, reserve);
            int loaded = Math.max(0, gun.useInventoryAmmo(stack) ? remaining : gun.getCurrentAmmoCount(stack));
            if (gun.hasBulletInBarrel(stack) && data != null && data.getBolt() != com.tacz.guns.resource.pojo.data.gun.Bolt.OPEN_BOLT
                    && loaded < Integer.MAX_VALUE) loaded++;
            return new View(loaded, remaining, unlimited, gun.getFireMode(stack).name().toLowerCase(java.util.Locale.ROOT));
        }

        private static final Method UNIDICT_MATCH = resolveUnidict();
        private static Method resolveUnidict() {
            if (!ModList.get().isLoaded("tacz_unidict")) return null;
            try {
                return Class.forName("com.scarasol.tud.manager.AmmoManager")
                        .getMethod("isAmmoOfGunItem", ItemStack.class, ItemStack.class);
            } catch (ReflectiveOperationException failure) {
                org.apache.logging.log4j.LogManager.getLogger().warn("Unidict HUD ammo adapter unavailable", failure);
                return null;
            }
        }
        private static boolean matches(ItemStack gun, ItemStack ammo) {
            if (UNIDICT_MATCH != null) {
                try { if ((boolean)UNIDICT_MATCH.invoke(null, gun, ammo)) return true; }
                catch (ReflectiveOperationException failure) { throw new IllegalStateException("Cannot read Unidict ammunition", failure); }
            }
            return ammo.getItem() instanceof com.tacz.guns.api.item.IAmmo item && item.isAmmoOfGun(gun, ammo);
        }
    }
    private WeaponAmmo() {}
}
