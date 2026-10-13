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
    private int[] displayLimits = InventorySlot.ALL.stream().mapToInt(InventorySlot::defaultLimit).toArray();
    private final LazyOptional<IItemHandler> ammunition = LazyOptional.of(() -> new IItemHandler() {
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
    });

    public PlayerPanelInventory(Player player) { this.player = player; }
    public static java.util.Optional<PlayerPanelInventory> get(Player player) { return player.getCapability(CAPABILITY).resolve(); }
    public boolean active() { return active && !pendingValidation; }
    public boolean hasSession() { return active; }
    public UUID round() { return round; }
    public int limit(InventorySlot slot) {
        if (player.level().isClientSide) return displayLimits[slot.index()];
        var stack = getStack(slot);
        return stack.isEmpty() ? rules.apply(slot).maximumLimit()
                : Math.min(stack.getMaxStackSize(), rules.apply(slot).limitFor(stack));
    }
    public PanelInventory storage() { return storage; }
    public LazyOptional<IItemHandler> ammunition() { return ammunition; }
    public void invalidate() { ammunition.invalidate(); }

    public void begin(BattlezoneMap map) {
        begin(map.getMapName(), map.getLootRoundId(), CapabilityMap.getMapCapability(map, BattlezoneInventoryCapability.class).orElseThrow()::rule);
    }

    public void begin(String name, UUID roundId, Function<InventorySlot, SlotRule> rules) {
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
        pendingValidation = false;
    }

    public void end() {
        active = false;
        pendingValidation = false;
        mapName = "";
        round = null;
        rules = InventoryRules.defaults().compile()::get;
        storage = new PanelInventory(slot -> rules.apply(slot));
    }

    public boolean manages(ItemStack stack) {
        return !stack.isEmpty() && (GunCompatManager.isGun(stack) || AmmoType.of(stack).tacz()
                || InventorySlot.AMMUNITION.stream().anyMatch(slot -> rules.apply(slot).accepts(stack))
                || InventorySlot.ITEMS.stream().anyMatch(slot -> rules.apply(slot).accepts(stack)));
    }

    /** Never mutates the input; callers own removing the accepted quantity from its source. */
    public ItemStack insert(ItemStack stack, boolean simulate) {
        if (!active() || stack.isEmpty()) return stack;
        if (!GunCompatManager.isGun(stack)) {
            if (AmmoType.of(stack).tacz() || InventorySlot.AMMUNITION.stream().anyMatch(slot -> rules.apply(slot).accepts(stack)))
                return storage.insertAmmo(stack, simulate);
            for (var slot : InventorySlot.ITEMS) {
                int index = 2 + InventorySlot.ITEMS.indexOf(slot);
                var existing = player.getInventory().getItem(index);
                if (!rules.apply(slot).accepts(stack) || (!existing.isEmpty() && !ItemStack.isSameItemSameTags(existing, stack))) continue;
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
            if (player.getInventory().getItem(i).isEmpty() && rule.accepts(stack)) {
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
                    : AmmoType.of(stack).tacz() || InventorySlot.AMMUNITION.stream().anyMatch(ammo -> rules.apply(ammo).accepts(stack)) || !rules.apply(slot).accepts(stack))) {
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
                boolean ammo = AmmoType.of(stack).tacz() || InventorySlot.AMMUNITION.stream().anyMatch(target -> rules.apply(target).accepts(stack));
                if ((i < 2 || !ammo) && rules.apply(slot).accepts(stack) && stack.getCount() <= Math.min(i < 2 ? 1 : stack.getMaxStackSize(), rules.apply(slot).limitFor(stack))) continue;
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
    }

    public List<ItemStack> snapshot() {
        return InventorySlot.ALL.stream().map(slot -> getStack(slot).copy()).toList();
    }

    public ItemStack getStack(InventorySlot slot) {
        if (InventorySlot.WEAPONS.contains(slot))
            return player.getInventory().getItem(slot.index() - InventorySlot.PRIMARY_WEAPON.index());
        if (InventorySlot.ITEMS.contains(slot)) return player.getInventory().getItem(2 + InventorySlot.ITEMS.indexOf(slot));
        return switch (slot) {
            case HELMET -> player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD);
            case VEST -> player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST);
            case SHIELD -> player.getOffhandItem();
            default -> storage.getStackInSlot(slot.index());
        };
    }

    /** Server-authoritative discard; the client cannot supply an item, quantity, or destination. */
    public boolean discard(InventorySlot slot, int action, UUID expectedRound, ItemStack expectedItem) {
        if (!active() || !java.util.Objects.equals(round, expectedRound) || action < 0 || action > 2) return false;
        var source = getStack(slot);
        if (source.isEmpty() || !ItemStack.isSameItemSameTags(source, expectedItem)) return false;
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
        player.getInventory().setChanged();
        return true;
    }
}
