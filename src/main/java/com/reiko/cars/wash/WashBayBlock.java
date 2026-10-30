package com.reiko.cars.wash;

import com.mojang.serialization.MapCodec;
import com.reiko.cars.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * A self-service Wash Bay: a coin-op pressure washer on a stand, with its hose on a reel and the lance in a holster,
 * under a WASH sign. Pay it (right-click) for some time, take the lance it hands you, and hold right-click to spray a
 * car clean (see {@link WashBayService}). Its owner right-clicks it for its price and books, and sneaks to take a lance.
 */
public final class WashBayBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final MapCodec<WashBayBlock> CODEC = simpleCodec(WashBayBlock::new);
    private static final VoxelShape SHAPE_X = Block.box(3, 0, 2, 13, 24, 14);
    private static final VoxelShape SHAPE_Z = Block.box(2, 0, 3, 14, 24, 13);

    public WashBayBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(FACING).getAxis() == Direction.Axis.Z ? SHAPE_Z : SHAPE_X;
    }

    /**
     * Where the hose comes off the reel, on the bay's left-hand side (looking at its front), as a point in the world.
     * The model's reel is at x 14, y 12, z 8 (pixels, facing north).
     */
    public static Vec3 outlet(BlockState state, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        double x = 14.5 / 16.0 - 0.5;
        double z = 8.0 / 16.0 - 0.5;
        // Turn the north-facing model's point to the block's facing (clockwise, as its blockstate does).
        int turns = switch (facing) {
            case EAST -> 1;
            case SOUTH -> 2;
            case WEST -> 3;
            default -> 0;
        };
        for (int i = 0; i < turns; i++) {
            double turned = -z;
            z = x;
            x = turned;
        }
        return new Vec3(pos.getX() + 0.5 + x, pos.getY() + 12.0 / 16.0, pos.getZ() + 0.5 + z);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        if (!level.isClientSide && placer instanceof Player player && level.getBlockEntity(pos) instanceof WashBayBlockEntity bay) {
            bay.setOwner(player);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof WashBayBlockEntity bay)) {
            return InteractionResult.PASS;
        }
        if (player instanceof ServerPlayer server) {
            WashBayService.use(server, bay);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WashBayBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != ModBlockEntities.WASH_BAY.get()) {
            return null;
        }
        return (tickLevel, pos, tickState, entity) -> {
            if (tickLevel.getGameTime() % 100 == 0) {
                ((WashBayBlockEntity) entity).expire(tickLevel.getGameTime());
            }
        };
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}
