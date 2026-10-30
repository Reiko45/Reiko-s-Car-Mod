package com.reiko.cars.charge;

import com.mojang.serialization.MapCodec;
import com.reiko.cars.registry.ModBlockEntities;
import java.util.EnumMap;
import java.util.Map;
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
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * A two-block-tall charging station for electric cars. The bottom half holds the {@link ChargingStationBlockEntity}
 * (its stored power, price and owner); the top half is there to be solid and clickable. Power goes in from BuildCraft
 * engines and kinesis pipes (or other mods' energy cables) on any side of either half. Right-click opens the charging
 * screen.
 */
public final class ChargingStationBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final MapCodec<ChargingStationBlock> CODEC = simpleCodec(ChargingStationBlock::new);
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;

    // Shapes for a station facing north, in pixels; rotated for the other directions.
    private static final Map<Direction, VoxelShape> LOWER = shapes(
            Block.box(1, 0, 2, 15, 2, 14), Block.box(3, 2, 5, 13, 16, 11), Block.box(12, 12, 6, 16, 16, 10));
    private static final Map<Direction, VoxelShape> UPPER = shapes(
            Block.box(3, 0, 5, 13, 15, 11), Block.box(12, 0, 6, 16, 3, 10));

    public ChargingStationBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(HALF, DoubleBlockHalf.LOWER));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, HALF);
    }

    private static Map<Direction, VoxelShape> shapes(VoxelShape... north) {
        Map<Direction, VoxelShape> map = new EnumMap<>(Direction.class);
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            VoxelShape shape = Shapes.empty();
            for (VoxelShape part : north) {
                shape = Shapes.or(shape, rotate(part, facing));
            }
            map.put(facing, shape.optimize());
        }
        return map;
    }

    /** Turns a north-facing box to face {@code facing}. */
    private static VoxelShape rotate(VoxelShape shape, Direction facing) {
        VoxelShape[] result = {Shapes.empty()};
        shape.forAllBoxes((x1, y1, z1, x2, y2, z2) -> {
            VoxelShape box = switch (facing) {
                case SOUTH -> Shapes.box(1 - x2, y1, 1 - z2, 1 - x1, y2, 1 - z1);
                case EAST -> Shapes.box(1 - z2, y1, x1, 1 - z1, y2, x2);
                case WEST -> Shapes.box(z1, y1, 1 - x2, z2, y2, 1 - x1);
                default -> Shapes.box(x1, y1, z1, x2, y2, z2);
            };
            result[0] = Shapes.or(result[0], box);
        });
        return result[0];
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return (state.getValue(HALF) == DoubleBlockHalf.LOWER ? LOWER : UPPER).get(state.getValue(FACING));
    }

    // ---- placing and breaking both halves together (as doors and the pumps do) ----

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        Level level = context.getLevel();
        if (pos.getY() < level.getMaxBuildHeight() - 1 && level.getBlockState(pos.above()).canBeReplaced(context)) {
            return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
        }
        return null;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        level.setBlock(pos.above(), state.setValue(HALF, DoubleBlockHalf.UPPER), Block.UPDATE_ALL);
        if (!level.isClientSide && placer instanceof Player player && level.getBlockEntity(pos) instanceof ChargingStationBlockEntity station) {
            station.setOwner(player);
        }
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level,
                                     BlockPos pos, BlockPos neighborPos) {
        DoubleBlockHalf half = state.getValue(HALF);
        if (direction.getAxis() == Direction.Axis.Y && (half == DoubleBlockHalf.LOWER) == (direction == Direction.UP)) {
            return neighbor.is(this) && neighbor.getValue(HALF) != half ? state : Blocks.AIR.defaultBlockState();
        }
        if (half == DoubleBlockHalf.LOWER && direction == Direction.DOWN && !state.canSurvive(level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighbor, level, pos, neighborPos);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (state.getValue(HALF) == DoubleBlockHalf.UPPER) {
            return level.getBlockState(pos.below()).is(this);
        }
        BlockPos below = pos.below();
        return level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
    }

    /** Breaking the top half breaks the bottom one properly, so the station (and its stored power) drops from there. */
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && state.getValue(HALF) == DoubleBlockHalf.UPPER) {
            BlockPos below = pos.below();
            if (level.getBlockState(below).is(this)) {
                boolean drop = !player.isCreative() && player.hasCorrectToolForDrops(level.getBlockState(below), level, below);
                level.destroyBlock(below, drop, player);
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }

    // ---- using it ----

    /** The station's block entity, from either half. */
    @Nullable
    public static ChargingStationBlockEntity stationAt(BlockGetter level, BlockPos pos, BlockState state) {
        if (!(state.getBlock() instanceof ChargingStationBlock)) {
            return null;
        }
        BlockPos lower = state.getValue(HALF) == DoubleBlockHalf.LOWER ? pos : pos.below();
        return level.getBlockEntity(lower) instanceof ChargingStationBlockEntity station ? station : null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        ChargingStationBlockEntity station = stationAt(level, pos, state);
        if (station == null) {
            return InteractionResult.PASS;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            ChargerMenu.open(serverPlayer, station);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    // ---- block entity ----

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER ? new ChargingStationBlockEntity(pos, state) : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != ModBlockEntities.CHARGING_STATION.get()) {
            return null;
        }
        return (tickLevel, pos, tickState, blockEntity) -> ((ChargingStationBlockEntity) blockEntity).serverTick();
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}
