package dev.stya.blockzone.loot;

import com.ptcrys.fpsmatch.common.entity.MatchDropEntity;
import dev.stya.blockzone.BlockZone;
import net.minecraft.world.entity.Entity;
import dev.stya.blockzone.map.battlezone.BattlezoneMap;
import dev.stya.blockzone.registry.BlockzoneEntities;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Shared match drops, including vanilla throws and drops produced by other mods. */
@Mod.EventBusSubscriber(modid = BlockZone.MOD_ID)
public final class MatchDrops {
    private record Pending(ItemEntity item, BattlezoneMap map) { }
    private static final java.util.Map<ServerLevel, java.util.List<Pending>> PENDING = new java.util.WeakHashMap<>();
    private MatchDrops() { }

    public static LootDropEntity create(Level level, ItemStack stack, BattlezoneMap map) {
        var drop = new LootDropEntity(BlockzoneEntities.LOOT_DROP.get(), level);
        drop.setItem(stack.copy());
        if (map != null) drop.bindToMatch(map);
        return drop;
    }

    /** Null means a short-lived visual pickup effect, not a real drop. */
    @javax.annotation.Nullable
    public static LootDropEntity copy(Entity item, BattlezoneMap map) {
        var stack = item instanceof ItemEntity vanilla ? vanilla.getItem() : ((MatchDropEntity) item).getItem();
        // Preserve thrower/owner, pickup delay, velocity, custom name and the complete stack NBT.
        var tag = item.saveWithoutId(new net.minecraft.nbt.CompoundTag());
        if (item instanceof ItemEntity vanilla && tag.getShort("PickupDelay") == 32767
                && vanilla.getAge() >= stack.getEntityLifespan(item.level()) - 1) return null;
        var drop = create(item.level(), stack, map);
        // FPSMatch drops extend Entity and do not save their stack or vanilla item fields.
        tag.put("Item", stack.save(new net.minecraft.nbt.CompoundTag()));
        if (!tag.contains("Health")) tag.putShort("Health", (short) 5);
        if (!tag.contains("PickupDelay")) tag.putShort("PickupDelay", (short) 20);
        drop.load(tag);
        drop.bindToMatch(map);
        return drop;
    }

    @SubscribeEvent
    public static void convert(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        var item = event.getEntity();
        if (item instanceof LootDropEntity || !(item instanceof ItemEntity || item instanceof MatchDropEntity)) return;
        var maps = LootCrateAccess.mapsAt(level, item.blockPosition()).stream()
                .filter(BattlezoneMap::isMatchActive).toList();
        if (maps.size() != 1) return;
        if (item instanceof ItemEntity vanilla) {
            // Callers such as /give set ownership or mark visual-only items AFTER spawning.
            // Wait until that call returns, then convert before the next entity tick/pickup.
            PENDING.computeIfAbsent(level, key -> new java.util.ArrayList<>()).add(new Pending(vanilla, maps.get(0)));
            return;
        }
        var drop = copy(item, maps.get(0));
        event.setCanceled(true);
        level.addFreshEntity(drop);
    }
    @SubscribeEvent
    public static void tick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.START || !(event.level instanceof ServerLevel level)) return;
        var pending = PENDING.remove(level);
        if (pending == null) return;
        for (var entry : pending) {
            var item = entry.item();
            if (!item.isAlive() || !item.isAddedToWorld() || !entry.map().isMatchActive()) continue;
            var drop = copy(item, entry.map());
            if (drop == null) continue;
            item.discard();
            level.addFreshEntity(drop);
        }
    }
}
