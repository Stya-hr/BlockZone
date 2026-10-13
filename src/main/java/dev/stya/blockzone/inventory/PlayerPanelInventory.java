package dev.stya.blockzone.inventory;

import com.ptcrys.fpsmatch.compat.gun.GunCompatManager;
import com.ptcrys.fpsmatch.core.capability.CapabilityMap;
import dev.stya.blockzone.map.battlezone.BattlezoneMap;
import dev.stya.blockzone.map.battlezone.capability.BattlezoneInventoryCapability;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;

/** Match-only reserve storage. Weapons have exactly one owner: vanilla hotbar slots 0 and 1. */
public final class PlayerPanelInventory {
    public static final Capability<PlayerPanelInventory> CAPABILITY = CapabilityManager.get(new CapabilityToken<>() {});
    private final Player player;
    private Function<InventorySlot, SlotRule> rules = InventoryRules.defaults().compile()::get;
    private PanelInventory storage = new PanelInventory(slot -> rules.apply(slot));
    private boolean active;
    private boolean pendingValidation;
    private String mapName = "";
    private UUID round;
    private net.minecraft.nbt.ListTag originalCuriosInventory = new net.minecraft.nbt.ListTag();
    private boolean hasOriginalCuriosInventory;
    private boolean restoreAccessoriesAfterDeath;
    private final ItemStack[] observedStacks = new ItemStack[InventorySlot.ALL.size()];
    private int[] slotRevisions = new int[InventorySlot.ALL.size()];
    private int[] displayLimits = InventorySlot.ALL.stream().mapToInt(InventorySlot::defaultLimit).toArray();
    private final IItemHandler ammunitionView = new IItemHandler() {
        private int index(int slot) {
            if (slot < 0 || slot >= getSlots()) throw new IllegalArgumentException("Invalid ammunition slot");
            return InventorySlot.AMMUNITION.get(slot).index();
        }
        @Override public int getSlots() { return InventorySlot.AMMUNITION.size(); }
        @Override public ItemStack getStackInSlot(int slot) { return storage.getStackInSlot(index(slot)); }
        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) { return storage.insertItem(index(slot), stack, simulate); }
        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) { return storage.extractItem(index(slot), amount, simulate); }
        @Override public int getSlotLimit(int slot) { return storage.getSlotLimit(index(slot)); }
        @Override public boolean isItemValid(int slot, ItemStack stack) { return storage.isItemValid(index(slot), stack); }
    };
    private LazyOptional<IItemHandler> ammunition = LazyOptional.of(() -> ammunitionView);

    public PlayerPanelInventory(Player player) { this.player = player; }
    public static java.util.Optional<PlayerPanelInventory> get(Player player) { return player.getCapability(CAPABILITY).resolve(); }
    public boolean active() { return active && !pendingValidation; }
    public boolean hasSession() { return active; }
    public UUID round() { return round; }
    /** Item identity, independent of ticking NBT or count changes; replaced stacks invalidate old clicks. */
    public int revision(InventorySlot slot) {
        if (!player.level().isClientSide) {
            var stack = getStack(slot);
            if (observedStacks[slot.index()] != stack) {
                observedStacks[slot.index()] = stack;
                slotRevisions[slot.index()]++;
            }
        }
        return slotRevisions[slot.index()];
    }
    public boolean accepts(InventorySlot slot, ItemStack stack) {
        return slot.acceptsType(stack) && rules.apply(slot).accepts(stack);
    }
    public int limit(InventorySlot slot) {
        if (player.level().isClientSide) return displayLimits[slot.index()];
        var stack = getStack(slot);
        if (InventorySlot.ACCESSORIES.contains(slot) || InventorySlot.WEAPONS.contains(slot) || InventorySlot.ARMOR.contains(slot)) return 1;
        return stack.isEmpty() ? rules.apply(slot).maximumLimit()
                : Math.min(stack.getMaxStackSize(), rules.apply(slot).limitFor(stack));
    }
    public PanelInventory storage() { return storage; }
    public LazyOptional<IItemHandler> ammunition() {
        if (!ammunition.isPresent()) ammunition = LazyOptional.of(() -> ammunitionView);
        return ammunition;
    }
    public void invalidate() { ammunition.invalidate(); }

    public void begin(BattlezoneMap map) {
        begin(map.getMapName(), map.getLootRoundId(), CapabilityMap.getMapCapability(map, BattlezoneInventoryCapability.class).orElseThrow()::rule);
    }

    public void begin(String name, UUID roundId, Function<InventorySlot, SlotRule> rules) {
        dev.stya.blockzone.equipment.AccessoryCompat.requireSlots(player);
        if (active) end();
        if (hasOriginalCuriosInventory) {
            restoreOriginalAccessories();
            if (hasOriginalCuriosInventory) throw new IllegalArgumentException("Curios is needed to restore the saved outside-match inventory");
        }
        var original = dev.stya.blockzone.equipment.AccessoryCompat.captureOriginal(player);
        originalCuriosInventory = original.orElseGet(net.minecraft.nbt.ListTag::new);
        hasOriginalCuriosInventory = original.isPresent();
        restoreAccessoriesAfterDeath = false;
        this.rules = java.util.Objects.requireNonNull(rules);
        storage = new PanelInventory(slot -> this.rules.apply(slot));
        mapName = name;
        round = java.util.Objects.requireNonNull(roundId);
        active = true;
        pendingValidation = false;
    }

    public boolean matches(BattlezoneMap map) {
        return active && mapName.equals(map.getMapName()) && java.util.Objects.equals(round, map.getLootRoundId());
    }

    public void resume(BattlezoneMap map) {
        rules = CapabilityMap.getMapCapability(map, BattlezoneInventoryCapability.class).orElseThrow()::rule;
        var external = dev.stya.blockzone.equipment.AccessoryCompat.inventory(player);
        if (external.isPresent()) for (var slot : InventorySlot.ACCESSORIES) {
            var saved = storage.getStackInSlot(slot.index());
            if (saved.isEmpty()) continue;
            storage.setStackInSlot(slot.index(), ItemStack.EMPTY);
            int index = InventorySlot.ACCESSORIES.indexOf(slot);
            if (index < external.get().getSlots() && external.get().getStackInSlot(index).isEmpty())
                external.get().setStackInSlot(index, saved);
            else returnRemainder(saved);
        }
        else for (var slot : InventorySlot.ACCESSORIES) {
            var saved = storage.getStackInSlot(slot.index());
            if (saved.getItem() instanceof dev.stya.blockzone.equipment.AccessoryItem item)
                item.onEquip(player, InventorySlot.ACCESSORIES.indexOf(slot), saved);
        }
        pendingValidation = false;
    }

    public void end() {
        if (active) for (var slot : InventorySlot.ACCESSORIES) setAccessory(slot, ItemStack.EMPTY);
        active = false;
        restoreOriginalAccessories();
        pendingValidation = false;
        mapName = "";
        round = null;
        rules = InventoryRules.defaults().compile()::get;
        storage = new PanelInventory(slot -> rules.apply(slot));
    }

    public void endAfterDeath() {
        var originals = originalCuriosInventory.copy();
        boolean hadOriginal = hasOriginalCuriosInventory;
        end();
        originalCuriosInventory = originals;
        hasOriginalCuriosInventory = hadOriginal;
        restoreAccessoriesAfterDeath = hadOriginal;
    }

    public void restoreOriginalAccessories() {
        if (hasOriginalCuriosInventory && !dev.stya.blockzone.equipment.AccessoryCompat.restoreOriginal(player, originalCuriosInventory)) {
            restoreAccessoriesAfterDeath = true;
            return;
        }
        originalCuriosInventory = new net.minecraft.nbt.ListTag();
        hasOriginalCuriosInventory = false;
        restoreAccessoriesAfterDeath = false;
    }

    private void setAccessory(InventorySlot slot, ItemStack stack) {
        int index = InventorySlot.ACCESSORIES.indexOf(slot);
        var external = dev.stya.blockzone.equipment.AccessoryCompat.inventory(player);
        if (external.isPresent() && index < external.get().getSlots()) external.get().setStackInSlot(index, stack.copy());
        else {
            var previous = storage.getStackInSlot(slot.index()).copy();
            storage.setStackInSlot(slot.index(), stack);
            if (previous.getItem() instanceof dev.stya.blockzone.equipment.AccessoryItem item) item.onUnequip(player, index, previous);
            if (stack.getItem() instanceof dev.stya.blockzone.equipment.AccessoryItem item) item.onEquip(player, index, stack);
        }
    }

    /** Exact destination used by the frozen initial-inventory capability. */
    public void equip(InventorySlot slot, ItemStack stack) {
        if (!active() || !accepts(slot, stack) || stack.getCount() > Math.min(stack.getMaxStackSize(), rules.apply(slot).limitFor(stack)))
            throw new IllegalArgumentException("Initial item violates slot rule: " + slot.id());
        if (InventorySlot.ACCESSORIES.contains(slot)) {
            if (stack.getCount() != 1 || !dev.stya.blockzone.equipment.AccessoryCompat.canEquip(player, InventorySlot.ACCESSORIES.indexOf(slot), stack))
                throw new IllegalArgumentException("Accessory cannot equip: " + slot.id());
            setAccessory(slot, stack);
        } else if (slot.equipmentSlot() != null) player.setItemSlot(slot.equipmentSlot(), stack.copy());
        else if (InventorySlot.WEAPONS.contains(slot)) player.getInventory().setItem(InventorySlot.WEAPONS.indexOf(slot), stack.copy());
        else if (InventorySlot.ITEMS.contains(slot)) player.getInventory().setItem(2 + InventorySlot.ITEMS.indexOf(slot), stack.copy());
        else storage.setStackInSlot(slot.index(), stack);
        player.getInventory().setChanged();
    }

    public void tickAccessories() {
        if (!active() || dev.stya.blockzone.equipment.AccessoryCompat.inventory(player).isPresent()) return;
        for (var slot : InventorySlot.ACCESSORIES) {
            var stack = storage.getStackInSlot(slot.index());
            if (stack.getItem() instanceof dev.stya.blockzone.equipment.AccessoryItem item)
                item.accessoryTick(player, InventorySlot.ACCESSORIES.indexOf(slot), stack);
        }
    }

    public ItemStack removeAccessory(InventorySlot slot) {
        var copy = getStack(slot).copy();
        setAccessory(slot, ItemStack.EMPTY);
        return copy;
    }

    public void restorePendingAccessories() {
        if (!active && restoreAccessoriesAfterDeath && player.isAlive()) restoreOriginalAccessories();
    }

    /** Called at LOWEST clone priority after Curios copied its handler and the original session ended. */
    public void restoreClonedAccessories(net.minecraft.nbt.ListTag stacks) {
        if (!hasSession() || !dev.stya.blockzone.equipment.AccessoryCompat.loaded()) return;
        var external = dev.stya.blockzone.equipment.AccessoryCompat.inventory(player);
        if (external.isEmpty()) return;
        // A third-party clone may have copied the original's restored outside inventory.
        // The original snapshot is already preserved above; only the two match items are live during the match.
        dev.stya.blockzone.equipment.AccessoryCompat.captureOriginal(player);
        for (int i = 0; i < Math.min(2, external.get().getSlots()); i++)
            external.get().setStackInSlot(i, i < stacks.size() ? ItemStack.of(stacks.getCompound(i)) : ItemStack.EMPTY);
    }

    public boolean manages(ItemStack stack) {
        return !stack.isEmpty() && (stack.getItem() instanceof net.minecraft.world.item.ArmorItem
                || dev.stya.blockzone.equipment.AccessoryCompat.isAccessory(stack) || GunCompatManager.isGun(stack) || AmmoType.of(stack).tacz()
                || InventorySlot.AMMUNITION.stream().anyMatch(slot -> accepts(slot, stack))
                || InventorySlot.ITEMS.stream().anyMatch(slot -> accepts(slot, stack)));
    }

    /** Never mutates the input; callers own removing the accepted quantity from its source. */
    public ItemStack insert(ItemStack stack, boolean simulate) {
        if (!active() || stack.isEmpty()) return stack;
        if (stack.getItem() instanceof net.minecraft.world.item.ArmorItem) {
            for (var slot : InventorySlot.ARMOR) {
                if (!getStack(slot).isEmpty() || !accepts(slot, stack)) continue;
                if (!simulate) { var equipped = stack.copy(); equipped.setCount(1); equip(slot, equipped); }
                var remainder = stack.copy(); remainder.shrink(1); return remainder;
            }
            return stack;
        }
        if (dev.stya.blockzone.equipment.AccessoryCompat.isAccessory(stack)) {
            for (var slot : InventorySlot.ACCESSORIES) {
                if (!getStack(slot).isEmpty() || !accepts(slot, stack)
                        || !dev.stya.blockzone.equipment.AccessoryCompat.canEquip(player, InventorySlot.ACCESSORIES.indexOf(slot), stack)) continue;
                if (!simulate) { var equipped = stack.copy(); equipped.setCount(1); setAccessory(slot, equipped); }
                var remainder = stack.copy(); remainder.shrink(1); return remainder;
            }
            return stack;
        }
        if (!GunCompatManager.isGun(stack)) {
            if (AmmoType.of(stack).tacz() || InventorySlot.AMMUNITION.stream().anyMatch(slot -> accepts(slot, stack)))
                return storage.insertAmmo(stack, simulate);
            for (var slot : InventorySlot.ITEMS) {
                int index = 2 + InventorySlot.ITEMS.indexOf(slot);
                var existing = player.getInventory().getItem(index);
                if (!accepts(slot, stack) || (!existing.isEmpty() && !ItemStack.isSameItemSameTags(existing, stack))) continue;
                int count = Math.min(stack.getCount(), Math.min(stack.getMaxStackSize(), rules.apply(slot).limitFor(stack)) - existing.getCount());
                if (count <= 0) continue;
                if (!simulate) {
                    var inserted = stack.copy(); inserted.setCount(existing.getCount() + count);
                    player.getInventory().setItem(index, inserted);
                    player.getInventory().setChanged();
                }
                var remainder = stack.copy(); remainder.shrink(count); return remainder;
            }
            return stack;
        }
        for (int i = 0; i < InventorySlot.WEAPONS.size(); i++) {
            var rule = rules.apply(InventorySlot.WEAPONS.get(i));
            if (player.getInventory().getItem(i).isEmpty() && accepts(InventorySlot.WEAPONS.get(i), stack)) {
                if (!simulate) {
                    var weapon = stack.copy();
                    weapon.setCount(1);
                    player.getInventory().setItem(i, weapon);
                    player.getInventory().setChanged();
                }
                var remainder = stack.copy();
                remainder.shrink(1);
                return remainder;
            }
        }
        return stack;
    }

    /** Reconcile starting loadouts and vanilla inventory/menu transfers without copying weapons. */
    public void normalize() {
        if (!active()) return;
        for (int i = 0; i < 6; i++) {
            var stack = player.getInventory().getItem(i);
            var slot = i < 2 ? InventorySlot.WEAPONS.get(i) : InventorySlot.ITEMS.get(i - 2);
            if (!stack.isEmpty() && (i < 2 ? !GunCompatManager.isGun(stack)
                    : AmmoType.of(stack).tacz() || InventorySlot.AMMUNITION.stream().anyMatch(ammo -> accepts(ammo, stack)) || !accepts(slot, stack))) {
                player.getInventory().setItem(i, ItemStack.EMPTY);
                if (manages(stack)) returnRemainder(insert(stack, false));
                else relocate(stack);
            }
        }
        for (int i = 0; i < player.getInventory().items.size(); i++) {
            var stack = player.getInventory().getItem(i);
            if (stack.isEmpty() || !manages(stack)) continue;
            if (i < 6) {
                var slot = i < 2 ? InventorySlot.WEAPONS.get(i) : InventorySlot.ITEMS.get(i - 2);
                boolean ammo = AmmoType.of(stack).tacz() || InventorySlot.AMMUNITION.stream().anyMatch(target -> accepts(target, stack));
                if ((i < 2 || !ammo) && accepts(slot, stack) && stack.getCount() <= Math.min(i < 2 ? 1 : stack.getMaxStackSize(), rules.apply(slot).limitFor(stack))) continue;
            }
            player.getInventory().setItem(i, ItemStack.EMPTY);
            returnRemainder(insert(stack, false));
        }
        var carried = player.containerMenu.getCarried();
        if (manages(carried)) {
            player.containerMenu.setCarried(ItemStack.EMPTY);
            returnRemainder(insert(carried, false));
        }
    }

    private void relocate(ItemStack stack) {
        // Reserve hotbar 0..5 for the two weapons and four item slots.
        var remainder = stack.copy();
        for (int i = 6; i < player.getInventory().items.size() && !remainder.isEmpty(); i++) {
            var existing = player.getInventory().getItem(i);
            if (!existing.isEmpty() && !ItemStack.isSameItemSameTags(existing, remainder)) continue;
            int count = Math.min(remainder.getCount(), remainder.getMaxStackSize() - existing.getCount());
            if (count <= 0) continue;
            var result = remainder.copy();
            result.setCount(existing.getCount() + count);
            player.getInventory().setItem(i, result);
            remainder.shrink(count);
        }
        returnRemainder(remainder);
    }

    private void returnRemainder(ItemStack stack) {
        if (stack.isEmpty()) return;
        if (player instanceof net.minecraft.server.level.ServerPlayer server) {
            var map = dev.stya.blockzone.combat.MatchRegeneration.map(server).filter(this::matches);
            if (map.isPresent()) { drop(map.get(), stack); return; }
        }
        player.drop(stack.copy(), false);
    }

    public void dropOnElimination(BattlezoneMap map) {
        for (var slot : InventorySlot.ACCESSORIES) {
            var stack = removeAccessory(slot);
            if (!stack.isEmpty()) drop(map, stack);
        }
        for (var slot : InventorySlot.AMMUNITION) {
            var stack = storage.getStackInSlot(slot.index());
            if (!stack.isEmpty()) {
                storage.setStackInSlot(slot.index(), ItemStack.EMPTY);
                drop(map, stack);
            }
        }
        for (int i = 0; i < 2; i++) {
            var stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && GunCompatManager.isGun(stack)) {
                player.getInventory().setItem(i, ItemStack.EMPTY);
                drop(map, stack);
            }
        }
        end();
    }

    private void drop(BattlezoneMap map, ItemStack stack) {
        var drop = new dev.stya.blockzone.loot.LootDropEntity(dev.stya.blockzone.registry.BlockzoneEntities.LOOT_DROP.get(), player.level());
        drop.setItem(stack.copy());
        drop.setPos(player.getX(), player.getY() + .2, player.getZ());
        drop.setDefaultPickUpDelay();
        drop.bindToMatch(map);
        player.level().addFreshEntity(drop);
    }

    public CompoundTag save() {
        var tag = new CompoundTag();
        tag.putBoolean("Active", active);
        tag.putString("Map", mapName);
        if (round != null) tag.putUUID("Round", round);
        tag.put("Inventory", storage.serializeNBT());
        tag.putIntArray("Limits", InventorySlot.ALL.stream().mapToInt(this::limit).toArray());
        tag.putIntArray("Revisions", InventorySlot.ALL.stream().mapToInt(this::revision).toArray());
        var accessories = new net.minecraft.nbt.ListTag();
        for (var slot : InventorySlot.ACCESSORIES) accessories.add(getStack(slot).save(new CompoundTag()));
        tag.put("AccessoryView", accessories);
        tag.put("OriginalCuriosInventory", originalCuriosInventory.copy());
        tag.putBoolean("HasOriginalCuriosInventory", hasOriginalCuriosInventory);
        tag.putBoolean("RestoreAccessories", restoreAccessoriesAfterDeath);
        return tag;
    }

    public void load(CompoundTag tag, boolean clientSync) {
        var loaded = new PanelInventory(slot -> rules.apply(slot));
        if (tag.contains("Inventory")) loaded.deserializeNBT(tag.getCompound("Inventory"));
        storage = loaded;
        mapName = tag.getString("Map");
        round = tag.hasUUID("Round") ? tag.getUUID("Round") : null;
        active = tag.getBoolean("Active") && round != null;
        pendingValidation = active && !clientSync;
        if (clientSync && tag.getIntArray("Limits").length == InventorySlot.ALL.size()) displayLimits = tag.getIntArray("Limits");
        if (clientSync && tag.getIntArray("Revisions").length == InventorySlot.ALL.size()) slotRevisions = tag.getIntArray("Revisions");
        originalCuriosInventory = tag.getList("OriginalCuriosInventory", net.minecraft.nbt.Tag.TAG_COMPOUND).copy();
        hasOriginalCuriosInventory = tag.getBoolean("HasOriginalCuriosInventory");
        restoreAccessoriesAfterDeath = tag.getBoolean("RestoreAccessories");
        if (clientSync || !dev.stya.blockzone.equipment.AccessoryCompat.loaded()) {
            var views = tag.getList("AccessoryView", net.minecraft.nbt.Tag.TAG_COMPOUND);
            // This is a client render snapshot, or recovery when an optional API was removed.
            // Server Curios inventories remain the only authoritative owner when installed.
            var contents = storage.serializeNBT();
            var items = contents.getList("Items", net.minecraft.nbt.Tag.TAG_COMPOUND);
            if (!views.isEmpty()) {
                for (int i = items.size() - 1; i >= 0; i--) {
                    int storedSlot = items.getCompound(i).getInt("Slot");
                    if (InventorySlot.ACCESSORIES.stream().anyMatch(slot -> slot.index() == storedSlot)) items.remove(i);
                }
                for (int i = 0; i < Math.min(2, views.size()); i++) {
                    var item = views.getCompound(i).copy();
                    item.putInt("Slot", InventorySlot.ACCESSORIES.get(i).index());
                    items.add(item);
                }
                contents.put("Items", items);
                storage.deserializeNBT(contents);
            }
        }
    }

    public List<ItemStack> snapshot() {
        return InventorySlot.ALL.stream().map(slot -> getStack(slot).copy()).toList();
    }

    public ItemStack getStack(InventorySlot slot) {
        if (InventorySlot.ACCESSORIES.contains(slot) && !player.level().isClientSide) {
            var external = dev.stya.blockzone.equipment.AccessoryCompat.inventory(player);
            int index = InventorySlot.ACCESSORIES.indexOf(slot);
            if (external.isPresent() && index < external.get().getSlots()) return external.get().getStackInSlot(index);
        }
        if (slot.equipmentSlot() != null) return player.getItemBySlot(slot.equipmentSlot());
        if (InventorySlot.WEAPONS.contains(slot))
            return player.getInventory().getItem(slot.index() - InventorySlot.PRIMARY_WEAPON.index());
        if (InventorySlot.ITEMS.contains(slot)) return player.getInventory().getItem(2 + InventorySlot.ITEMS.indexOf(slot));
        return storage.getStackInSlot(slot.index());
    }

    /** Server-authoritative discard; the client cannot supply an item, quantity, or destination. */
    public boolean discard(InventorySlot slot, int action, UUID expectedRound, int expectedRevision) {
        return revision(slot) == expectedRevision && discard(slot, action, expectedRound, getStack(slot).copy());
    }

    public boolean discard(InventorySlot slot, int action, UUID expectedRound, ItemStack expectedItem) {
        if (!active() || !java.util.Objects.equals(round, expectedRound) || action < 0 || action > 2) return false;
        var source = getStack(slot);
        if (source.isEmpty() || !ItemStack.isSameItemSameTags(source, expectedItem)) return false;
        if (InventorySlot.ACCESSORIES.contains(slot) && !dev.stya.blockzone.equipment.AccessoryCompat.canUnequip(player, InventorySlot.ACCESSORIES.indexOf(slot), source)) return false;
        int count = Math.min(source.getCount(), action == 0 ? 1 : action == 1 ? 10 : source.getCount());
        var dropped = source.copy(); dropped.setCount(count);
        source.shrink(count);
        var entity = new dev.stya.blockzone.loot.LootDropEntity(dev.stya.blockzone.registry.BlockzoneEntities.LOOT_DROP.get(), player.level());
        entity.setItem(dropped);
        entity.setPos(player.getX(), player.getEyeY() - .3, player.getZ());
        entity.setDeltaMovement(player.getLookAngle().scale(.25).add(0, .1, 0));
        entity.setPickUpDelay(20);
        if (player instanceof net.minecraft.server.level.ServerPlayer server)
            dev.stya.blockzone.combat.MatchRegeneration.map(server).filter(this::matches).ifPresent(entity::bindToMatch);
        if (!player.level().addFreshEntity(entity)) { source.grow(count); return false; }
        if (InventorySlot.ACCESSORIES.contains(slot) && source.isEmpty()
                && dev.stya.blockzone.equipment.AccessoryCompat.inventory(player).isEmpty()
                && dropped.getItem() instanceof dev.stya.blockzone.equipment.AccessoryItem accessory)
            accessory.onUnequip(player, InventorySlot.ACCESSORIES.indexOf(slot), dropped);
        player.getInventory().setChanged();
        return true;
    }
}
