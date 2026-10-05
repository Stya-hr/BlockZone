package dev.stya.blockzone.loot;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;

public final class LootCrateBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
    private final VoxelShape[] closed = new VoxelShape[4];
    private final VoxelShape[] opened = new VoxelShape[4];

    public LootCrateBlock() { this(-4, 20, 0, 16, 8, 2.5, .5); }

    public LootCrateBlock(double minX, double maxX, double minZ, double maxZ,
                          double bodyHeight, double lidThickness, double relief) {
        super(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).strength(3.0F, 3_600_000.0F)
                .sound(SoundType.METAL).noOcclusion().pushReaction(PushReaction.BLOCK));
        double handle=(maxX-minX)/28+.05;
        closed[0] = Block.box(minX-handle, 0, minZ-.2, maxX+handle, bodyHeight+lidThickness+relief, maxZ+.2);
        var base = Block.box(minX-handle, 0, minZ-.2, maxX+handle, bodyHeight+lidThickness/3, maxZ+.2);
        var lid = Block.box(minX-.08, bodyHeight-.08, maxZ-lidThickness,
                maxX+.08, bodyHeight+maxZ-minZ+.08, maxZ+relief+.08);
        opened[0] = Shapes.or(base, lid);
        for (int i = 1; i < 4; i++) {
            closed[i] = rotateShape(closed[i-1]);
            opened[i] = rotateShape(opened[i-1]);
        }
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(OPEN, false));
    }

    public static VoxelShape rotateShape(VoxelShape shape) {
        VoxelShape rotated = Shapes.empty();
        for (var box : shape.toAabbs()) {
            rotated = Shapes.or(rotated, Shapes.box(1-box.maxZ, box.minY, box.minX,
                    1-box.minZ, box.maxY, box.maxX));
        }
        return rotated;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, OPEN);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int rotation = rotation(state.getValue(FACING));
        return defaultShape(state.getValue(OPEN),rotation);
    }

    public VoxelShape defaultShape(boolean open,int rotation) { return (open?opened:closed)[rotation]; }

    public static int rotation(Direction direction) {
        return switch(direction) { case EAST -> 1; case SOUTH -> 2; case WEST -> 3; default -> 0; };
    }
    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return rotate(state, mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LootCrateBlockEntity(pos, state);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide) {
            var container=level.getBlockEntity(pos);
            if(player instanceof net.minecraft.server.level.ServerPlayer serverPlayer && LootContainerControl.interact(container,serverPlayer))
                return InteractionResult.CONSUME;
            if(container instanceof LootCrateBlockEntity crate) player.openMenu(crate);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void tick(BlockState state,net.minecraft.server.level.ServerLevel level,BlockPos pos,net.minecraft.util.RandomSource random) {
        if(level.getBlockEntity(pos) instanceof LootCrateBlockEntity crate) crate.recheckOpen();
    }

    @Override
    public void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving) {
        if(!state.is(next.getBlock())) {
            if(level.getBlockEntity(pos) instanceof LootCrateBlockEntity crate) {
                net.minecraft.world.Containers.dropContents(level,pos,crate); level.updateNeighbourForOutputSignal(pos,this);
            }
            super.onRemove(state,level,pos,next,moving);
        }
    }
}
