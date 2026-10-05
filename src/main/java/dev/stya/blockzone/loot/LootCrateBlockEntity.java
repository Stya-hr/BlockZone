package dev.stya.blockzone.loot;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** An ordinary 27-slot container. Map loot behavior belongs to LootContainerControl. */
public final class LootCrateBlockEntity extends RandomizableContainerBlockEntity {
    private NonNullList<ItemStack> items = NonNullList.withSize(27, ItemStack.EMPTY);
    private final ContainerOpenersCounter openers = new ContainerOpenersCounter() {
        @Override protected void onOpen(net.minecraft.world.level.Level level,BlockPos pos,BlockState state) { setOpen(true); level.playSound(null,pos,net.minecraft.sounds.SoundEvents.CHEST_OPEN,net.minecraft.sounds.SoundSource.BLOCKS,1,.85F); }
        @Override protected void onClose(net.minecraft.world.level.Level level,BlockPos pos,BlockState state) { setOpen(false); level.playSound(null,pos,net.minecraft.sounds.SoundEvents.CHEST_CLOSE,net.minecraft.sounds.SoundSource.BLOCKS,1,.85F); }
        @Override protected void openerCountChanged(net.minecraft.world.level.Level level,BlockPos pos,BlockState state,int before,int after) { }
        @Override protected boolean isOwnContainer(Player player) {
            return player.containerMenu instanceof ChestMenu menu && menu.getContainer()==LootCrateBlockEntity.this;
        }
    };
    public LootCrateBlockEntity(BlockPos pos,BlockState state) { super(LootCrateRegistry.LOOT_CRATE_ENTITY.get(),pos,state); }
    @Override public int getContainerSize() { return 27; }
    @Override protected NonNullList<ItemStack> getItems() { return items; }
    @Override protected void setItems(NonNullList<ItemStack> items) { this.items=items; }
    @Override protected Component getDefaultName() { return Component.translatable(getBlockState().getBlock().getDescriptionId()); }
    @Override protected AbstractContainerMenu createMenu(int id,Inventory inventory) { return ChestMenu.threeRows(id,inventory,this); }
    @Override public void startOpen(Player player) {
        if(!isRemoved() && !player.isSpectator()) openers.incrementOpeners(player,level,worldPosition,getBlockState());
    }
    @Override public void stopOpen(Player player) {
        if(!isRemoved() && !player.isSpectator()) openers.decrementOpeners(player,level,worldPosition,getBlockState());
    }
    public void recheckOpen() { if(!isRemoved()) openers.recheckOpeners(level,worldPosition,getBlockState()); }
    private void setOpen(boolean value) {
        if(level!=null && !level.isClientSide) {
            if(LootContainerControl.isControlled(this)) value=LootContainerControl.data(this).opened();
            level.setBlock(worldPosition,getBlockState().setValue(LootCrateBlock.OPEN,value),Block.UPDATE_ALL);
        }
    }
    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if(!trySaveLootTable(tag)) ContainerHelper.saveAllItems(tag,items);
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag); items=NonNullList.withSize(getContainerSize(),ItemStack.EMPTY);
        if(!tryLoadLootTable(tag)) ContainerHelper.loadAllItems(tag,items);
    }
    @Override public void onLoad() {
        super.onLoad();
        if(level!=null && !level.isClientSide) setOpen(LootContainerControl.isControlled(this) && LootContainerControl.data(this).opened());
    }
}
