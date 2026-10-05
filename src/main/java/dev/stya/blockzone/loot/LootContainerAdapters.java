package dev.stya.blockzone.loot;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Opt-in container bridge. Register a dedicated adapter for unusual inventories or animations. */
public final class LootContainerAdapters {
    public static final TagKey<Block> CONTAINERS=TagKey.create(net.minecraft.core.registries.Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath("blockzone","loot_containers"));
    public interface Adapter {
        boolean supports(BlockEntity container);
        default Direction facing(BlockEntity container) {
            var state=container.getBlockState();
            if(state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) return state.getValue(BlockStateProperties.HORIZONTAL_FACING);
            if(state.hasProperty(BlockStateProperties.FACING)) {
                var direction=state.getValue(BlockStateProperties.FACING);
                if(direction.getAxis().isHorizontal()) return direction;
            }
            return Direction.NORTH;
        }
        default void setOpened(BlockEntity container,boolean opened) {
            var level=container.getLevel(); var state=container.getBlockState();
            if(level==null || level.isClientSide) return;
            if(state.hasProperty(BlockStateProperties.OPEN)) level.setBlock(container.getBlockPos(),state.setValue(BlockStateProperties.OPEN,opened),Block.UPDATE_ALL);
            if(container instanceof ChestBlockEntity) level.blockEvent(container.getBlockPos(),state.getBlock(),1,opened?1:0);
        }
    }
    private static final List<Adapter> CUSTOM=new CopyOnWriteArrayList<>();
    private static final Adapter STANDARD=new Adapter() {
        @Override public boolean supports(BlockEntity container) {
            var state=container.getBlockState();
            if(!state.is(CONTAINERS)) return false;
            // Linked/multiblock containers need a dedicated adapter to define their logical owner.
            if(state.hasProperty(BlockStateProperties.CHEST_TYPE) && state.getValue(BlockStateProperties.CHEST_TYPE)!=ChestType.SINGLE) return false;
            return container instanceof Container || container.getCapability(ForgeCapabilities.ITEM_HANDLER).isPresent();
        }
    };
    private LootContainerAdapters() { }
    public static void register(Adapter adapter) { CUSTOM.add(java.util.Objects.requireNonNull(adapter)); }
    public static Adapter find(BlockEntity container) {
        if(container==null || container.isRemoved()) return null;
        for(var adapter:CUSTOM) if(adapter.supports(container)) return adapter;
        return STANDARD.supports(container)?STANDARD:null;
    }
}
