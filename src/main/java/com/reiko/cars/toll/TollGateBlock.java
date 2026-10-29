package com.reiko.cars.toll;

import com.mojang.serialization.MapCodec;
import com.reiko.cars.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * A toll gate: a post with a striped barrier arm across three blocks of road beside it (to the right of whoever put it
 * down, facing them). Closed, the arm's blocks are a wall a block and a half high that cars can't climb; open, the arm
 * swings up and the road's clear. A car's driver opens it by paying at the gate (G, see {@link TollService}); its
 * owner can also right-click it to hold it open, or shut it again.
 */
public final class TollGateBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final MapCodec<TollGateBlock> CODEC = simpleCodec(TollGateBlock::new);
    public static final EnumProperty<GatePart> PART = EnumProperty.create("part", GatePart.class);
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
    /** How many blocks the arm spans beside the pole. */
    public static final int ARMS = 3;

    private static final VoxelShape POLE = Block.box(5, 0, 5, 11, 20, 11);
    private static final VoxelShape WALL_X = Block.box(0, 0, 6.5, 16, 24, 9.5);
    private static final VoxelShape WALL_Z = Block.box(6.5, 0, 0, 9.5, 24, 16);

    public TollGateBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(PART, GatePart.POLE).setValue(OPEN, false));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART, OPEN);
    }

    /** Which way the arm reaches from the pole. */
    public static Direction armDirection(BlockState state) {
        return state.getValue(FACING).getCounterClockWise();
    }

    /** The pole of the gate the block at {@code pos} belongs to. */
    public static BlockPos polePos(BlockState state, BlockPos pos) {
        return pos.relative(armDirection(state), -state.getValue(PART).index());
    }

    /** The closed arm's wall in a block, across the road. */
    public static VoxelShape wall(BlockState state) {
        return armDirection(state).getAxis() == Direction.Axis.X ? WALL_X : WALL_Z;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        boolean open = state.getValue(OPEN);
        if (state.getValue(PART) == GatePart.POLE) {
            return open ? POLE : Shapes.or(POLE, wall(state));
        }
        return open ? Shapes.empty() : wall(state);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        BlockState state = defaultBlockState().setValue(FACING, facing);
        Direction arm = armDirection(state);
        Level level = context.getLevel();
        for (int i = 1; i <= ARMS; i++) {
            BlockPos at = context.getClickedPos().relative(arm, i);
            if (!level.getBlockState(at).canBeReplaced(context) || !level.getWorldBorder().isWithinBounds(at)) {
                if (!level.isClientSide && context.getPlayer() != null) {
                    context.getPlayer().displayClientMessage(Component.translatable("toll.reikocars.no_room"), true);
                }
                return null;
            }
        }
        return state;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        if (level.isClientSide) {
            return;
        }
        Direction arm = armDirection(state);
        for (int i = 1; i <= ARMS; i++) {
            level.setBlock(pos.relative(arm, i), state.setValue(PART, GatePart.at(i)), Block.UPDATE_ALL);
        }
        if (placer instanceof Player player && level.getBlockEntity(pos) instanceof TollGateBlockEntity gate) {
            gate.setOwner(player);
        }
    }

    /** Breaking any block of the gate takes the rest with it (one gate comes back). */
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !level.isClientSide) {
            BlockPos pole = polePos(state, pos);
            Direction arm = armDirection(state);
            for (int i = 0; i <= ARMS; i++) {
                BlockPos at = pole.relative(arm, i);
                BlockState other = level.getBlockState(at);
                if (!at.equals(pos) && other.is(this) && other.getValue(PART).index() == i && other.getValue(FACING) == state.getValue(FACING)) {
                    level.setBlock(at, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
                }
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        BlockPos pole = polePos(state, pos);
        if (!(level.getBlockEntity(pole) instanceof TollGateBlockEntity gate)) {
            return InteractionResult.PASS;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            if (gate.canManage(player) && gate.owner() != null) {
                TollService.toggleHeld(serverPlayer, (ServerLevel) level, pole);
            } else {
                serverPlayer.displayClientMessage(Component.translatable("toll.reikocars.pay_at_gate"), true);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** The pole's scheduled tick: time to shut, if nothing's in the way. */
    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(PART) == GatePart.POLE) {
            TollService.tryClose(level, pos);
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(PART) == GatePart.POLE ? new TollGateBlockEntity(pos, state) : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (!level.isClientSide || type != ModBlockEntities.TOLL_GATE.get()) {
            return null;
        }
        return (tickLevel, pos, tickState, blockEntity) -> ((TollGateBlockEntity) blockEntity).clientTick();
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return state.getValue(PART) == GatePart.POLE ? RenderShape.MODEL : RenderShape.INVISIBLE;
    }
}
