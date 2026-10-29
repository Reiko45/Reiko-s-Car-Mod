package com.reiko.cars.insurance;

import com.mojang.serialization.MapCodec;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * An insurance office's desk, under an INSURANCE sign. Car owners right-click it to insure the cars they've parked
 * nearby; its owner sets the premium, deductible and claim limit, and pays the claims.
 */
public final class InsuranceOfficeBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final MapCodec<InsuranceOfficeBlock> CODEC = simpleCodec(InsuranceOfficeBlock::new);
    private static final VoxelShape SHAPE = Shapes.or(Block.box(0, 0, 2, 16, 12, 14), Block.box(1, 12, 9, 15, 24, 11));
    private static final VoxelShape SHAPE_X = Shapes.or(Block.box(2, 0, 0, 14, 12, 16), Block.box(5, 12, 1, 7, 24, 15));
    private static final VoxelShape SHAPE_Z_SOUTH = Shapes.or(Block.box(0, 0, 2, 16, 12, 14), Block.box(1, 12, 5, 15, 24, 7));
    private static final VoxelShape SHAPE_X_EAST = Shapes.or(Block.box(2, 0, 0, 14, 12, 16), Block.box(9, 12, 1, 11, 24, 15));

    public InsuranceOfficeBlock(Properties properties) {
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

    /** The desk, and the sign behind it (at the back, away from the customer's side). */
    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case SOUTH -> SHAPE_Z_SOUTH;
            case WEST -> SHAPE_X_EAST;
            case EAST -> SHAPE_X;
            default -> SHAPE;
        };
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        if (!level.isClientSide && placer instanceof Player player && level.getBlockEntity(pos) instanceof InsuranceOfficeBlockEntity office) {
            office.setOwner(player);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof InsuranceOfficeBlockEntity office)) {
            return InteractionResult.PASS;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            InsuranceMenu.open(serverPlayer, office);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new InsuranceOfficeBlockEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}
