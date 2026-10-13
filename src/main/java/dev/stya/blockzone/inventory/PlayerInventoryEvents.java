package dev.stya.blockzone.inventory;

import dev.stya.blockzone.BlockZone;
import dev.stya.blockzone.combat.MatchRegeneration;
import dev.stya.blockzone.net.battlezone.BattlezoneNetwork;
import dev.stya.blockzone.net.battlezone.InventoryStateS2CPacket;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BlockZone.MOD_ID)
public final class PlayerInventoryEvents {
    private static final Map<ServerPlayer, CompoundTag> LAST = new WeakHashMap<>();

    @SubscribeEvent public static void attach(AttachCapabilitiesEvent<Entity> event) {
        if (!(event.getObject() instanceof Player player)) return;
        var data = new PlayerPanelInventory(player);
        class Provider implements ICapabilitySerializable<CompoundTag> {
            private LazyOptional<PlayerPanelInventory> optional = LazyOptional.of(() -> data);
            @Override public <T> LazyOptional<T> getCapability(Capability<T> type, Direction side) {
                if (type != PlayerPanelInventory.CAPABILITY) return LazyOptional.empty();
                // Entity.reviveCaps can be called after another mod already invalidated its lazy views during cloning.
                // The entity gates capability access while invalid; renew only when the provider is queried again.
                if (!optional.isPresent()) optional = LazyOptional.of(() -> data);
                return optional.cast();
            }
            @Override public CompoundTag serializeNBT() { return data.save(); }
            @Override public void deserializeNBT(CompoundTag nbt) { data.load(nbt, false); }
            void invalidate() { optional.invalidate(); data.invalidate(); }
        }
        var provider = new Provider();
        event.addCapability(ResourceLocation.fromNamespaceAndPath(BlockZone.MOD_ID, "inventory"), provider);
        event.addListener(provider::invalidate);
    }

    @SubscribeEvent(priority = net.minecraftforge.eventbus.api.EventPriority.LOWEST)
    public static void clonePlayer(PlayerEvent.Clone event) {
        // Match deaths normally become elimination, but dimension/respawn clones must not duplicate reserves.
        event.getOriginal().reviveCaps();
        try {
            PlayerPanelInventory.get(event.getOriginal()).ifPresent(old -> PlayerPanelInventory.get(event.getEntity()).ifPresent(next -> {
                var state = old.save();
                next.load(state, false);
                old.end();
                next.restoreClonedAccessories(state.getList("AccessoryView", net.minecraft.nbt.Tag.TAG_COMPOUND));
            }));
        } finally { event.getOriginal().invalidateCaps(); }
    }

    @SubscribeEvent(priority = net.minecraftforge.eventbus.api.EventPriority.LOWEST)
    public static void actualDeath(net.minecraftforge.event.entity.living.LivingDropsEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        PlayerPanelInventory.get(player).filter(PlayerPanelInventory::active).ifPresent(data -> {
            for (var slot : InventorySlot.ACCESSORIES) {
                var stack = data.removeAccessory(slot);
                if (!stack.isEmpty()) event.getDrops().add(new net.minecraft.world.entity.item.ItemEntity(
                        player.level(), player.getX(), player.getY(), player.getZ(), stack));
            }
            for (var slot : InventorySlot.AMMUNITION) {
                var stack = data.storage().extractItem(slot.index(), Integer.MAX_VALUE, false);
                if (!stack.isEmpty()) event.getDrops().add(new net.minecraft.world.entity.item.ItemEntity(
                        player.level(), player.getX(), player.getY(), player.getZ(), stack));
            }
            data.endAfterDeath();
        });
    }

    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.player.level().isClientSide) {
            PlayerPanelInventory.get(event.player).ifPresent(PlayerPanelInventory::tickAccessories);
            return;
        }
        if (!(event.player instanceof ServerPlayer player)) return;
        PlayerPanelInventory.get(player).ifPresent(data -> {
            data.restorePendingAccessories();
            if (data.hasSession()) {
                var map = MatchRegeneration.map(player).filter(data::matches)
                        .filter(m -> !MatchRegeneration.allowed(player));
                if (map.isPresent()) {
                    if (!data.active()) data.resume(map.get());
                    data.normalize();
                }
                else data.end();
            }
            data.tickAccessories();
            var tag = data.save();
            // Weapons already synchronize through vanilla inventory packets. Reserves need their own snapshot.
            if (!tag.equals(LAST.get(player))) {
                LAST.put(player, tag.copy());
                BattlezoneNetwork.sendEditor(player, new InventoryStateS2CPacket(tag));
            }
        });
    }

    @Mod.EventBusSubscriber(modid = BlockZone.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        @SubscribeEvent public static void register(RegisterCapabilitiesEvent event) { event.register(PlayerPanelInventory.class); }
    }
}
