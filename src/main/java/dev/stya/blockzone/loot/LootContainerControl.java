package dev.stya.blockzone.loot;

import com.mojang.logging.LogUtils;
import dev.stya.blockzone.editor.loot.LootCrateEdit;
import dev.stya.blockzone.map.battlezone.BattlezoneMap;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.*;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

/** Map-assigned behavior on an existing container, independent of the container's inventory/model. */
public final class LootContainerControl {
    public static final String KEY="BlockzoneLoot";
    public static final String DEFAULT_TABLE="blockzone:chests/common";
    private LootContainerControl() { }
    public static LootCrateEdit data(BlockEntity container) {
        var tag=container.getPersistentData().getCompound(KEY); var pos=container.getBlockPos();
        String table=tag.getString("LootTable");
        if(ResourceLocation.tryParse(table)==null) table=DEFAULT_TABLE;
        return new LootCrateEdit(pos.getX(),pos.getY(),pos.getZ(),table,tag.getLong("LootTableSeed"),tag.getBoolean("Enabled") && tag.getBoolean("Opened"),
                tag.getBoolean("Enabled"),ForgeRegistries.BLOCKS.getKey(container.getBlockState().getBlock()).toString());
    }
    private static BattlezoneMap owner(BlockEntity container) {
        if(container==null || !(container.getLevel() instanceof ServerLevel level) || !data(container).enabled() || LootContainerAdapters.find(container)==null) return null;
        String name=container.getPersistentData().getCompound(KEY).getString("Map");
        var maps=LootCrateAccess.mapsAt(level,container.getBlockPos());
        return maps.size()==1 && maps.get(0).getMapName().equals(name)?maps.get(0):null;
    }
    public static boolean isControlled(BlockEntity container) { return container!=null && owner(container)!=null; }
    public static void apply(BlockEntity container,LootCrateEdit entry,String mapName) {
        var adapter=LootContainerAdapters.find(container);
        if(adapter==null) throw new IllegalArgumentException("Unsupported container");
        var tag=new CompoundTag(); tag.putString("Map",mapName); tag.putBoolean("Enabled",entry.enabled());
        tag.putString("LootTable",entry.table()); tag.putLong("LootTableSeed",entry.seed()); tag.putBoolean("Opened",entry.enabled() && entry.opened());
        container.getPersistentData().put(KEY,tag); container.setChanged();
        adapter.setOpened(container,entry.enabled() && entry.opened());
    }
    /** Returns true whenever the normal container interaction must be consumed. */
    public static boolean interact(BlockEntity container,ServerPlayer player) {
        var map=owner(container);
        if(map==null) return false;
        var entry=data(container);
        if(!LootCrateAccess.mayLoot(map,player)) {
            player.displayClientMessage(Component.translatable("blockzone.loot_crate.unavailable"),true); return true;
        }
        if(entry.opened() || player.distanceToSqr(Vec3.atCenterOf(container.getBlockPos()))>64) return true;
        var level=(ServerLevel)container.getLevel();
        var id=ResourceLocation.tryParse(entry.table());
        var table=level.getServer().getLootData().getElement(new LootDataId<>(LootDataType.TABLE,id));
        if(table==null) { player.displayClientMessage(Component.translatable("blockzone.loot_crate.missing_table",entry.table()),true); return true; }
        List<ItemStack> rewards;
        try {
            var params=new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN,Vec3.atCenterOf(container.getBlockPos()))
                    .withParameter(LootContextParams.THIS_ENTITY,player).create(LootContextParamSets.CHEST);
            rewards=table.getRandomItems(params,entry.seed()==0?level.random.nextLong():entry.seed());
        } catch(RuntimeException failure) {
            LogUtils.getLogger().error("Cannot generate loot for container at {}",container.getBlockPos(),failure);
            player.displayClientMessage(Component.translatable("blockzone.loot_crate.invalid_table",entry.table()),true); return true;
        }
        var adapter=LootContainerAdapters.find(container);
        var facing=adapter.facing(container);
        double forward=Math.atan2(facing.getStepZ(),facing.getStepX());
        var shape=container.getBlockState().getShape(level,container.getBlockPos());
        double height=shape.isEmpty()?.85:Math.max(.85,shape.bounds().maxY+.25);
        // Consume first, so repeated interactions cannot duplicate a loot roll.
        apply(container,new LootCrateEdit(entry.x(),entry.y(),entry.z(),entry.table(),entry.seed(),true,true,entry.block()),map.getMapName());
        var pos=container.getBlockPos();
        for(int i=0;i<rewards.size();i++) {
            if(rewards.get(i).isEmpty()) continue;
            var drop=new LootDropEntity(LootCrateRegistry.LOOT_DROP.get(),level); drop.setItem(rewards.get(i).copy());
            drop.setPos(pos.getX()+.5+facing.getStepX()*.25,pos.getY()+height,pos.getZ()+.5+facing.getStepZ()*.25);
            double angle=forward+(rewards.size()==1?0:(double)i/(rewards.size()-1)-.5)*Math.PI*.8;
            drop.setDeltaMovement(Math.cos(angle)*.22,.28+level.random.nextDouble()*.08,Math.sin(angle)*.22);
            drop.setPickUpDelay(20); drop.bindToMatch(map); level.addFreshEntity(drop);
        }
        level.playSound(null,pos,SoundEvents.CHEST_OPEN,SoundSource.BLOCKS,1,.85F); level.gameEvent(player,GameEvent.CONTAINER_OPEN,pos);
        return true;
    }
}
