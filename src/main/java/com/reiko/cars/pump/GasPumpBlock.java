package com.reiko.cars.pump;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.reiko.cars.car.FuelType;
import com.reiko.cars.registry.ModBlockEntities;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
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
import net.neoforged.neoforge.fluids.FluidUtil;
import org.jetbrains.annotations.Nullable;

/**
 * A two-block-tall fuel pump: the gas pump or the diesel pump, which work the same way but sell different fuel
 * ({@link #fuel()}). The bottom half holds the {@link GasPumpBlockEntity} (the fuel, price and owner); the top half is
 * just there to be solid and clickable. Fuel goes in through pipes on any side of either half, or from a bucket by
 * the owner. Right-click opens the pump screen.
 */
public final class GasPumpBlock extends HorizontalDirectionalBlock implements EntityBlock {
    private static final Codec<FuelType> FUEL_CODEC = Codec.STRING.xmap(name -> FuelType.valueOf(name.toUpperCase(Locale.ROOT)), FuelType::id);
    public static final MapCodec<GasPumpBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            FUEL_CODEC.fieldOf("fuel").forGetter(GasPumpBlock::fuel),
            propertiesCodec()
    ).apply(instance, GasPumpBlock::new));
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;

    // Shapes for a pump facing north, in pixels; rotated for the other directions.
    private static final Map<Direction, VoxelShape> LOWER = shapes(
            Block.box(1, 0, 2, 15, 2, 14), Block.box(2, 2, 4, 14, 16, 12), Block.box(14, 4, 6, 16, 16, 10));
    private static final Map<Direction, VoxelShape> UPPER = shapes(
            Block.box(2, 0, 4, 14, 12, 12), Block.box(1, 12, 3, 15, 16, 13));

    private final FuelType fuel;

    public GasPumpBlock(FuelType fuel, Properties properties) {
        super(properties);
        this.fuel = fuel;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(HALF, DoubleBlockHalf.LOWER));
    }

    /** What this pump sells. */
    public FuelType fuel() {
        return fuel;
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

    // ---- placing and breaking both halves together (as doors do) ----

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
        if (!level.isClientSide && placer instanceof Player player && level.getBlockEntity(pos) instanceof GasPumpBlockEntity pump) {
            pump.setOwner(player);
        }
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level,
                                     BlockPos pos, BlockPos neighborPos) {
        DoubleBlockHalf half = state.getValue(HALF);
        if (direction.getAxis() == Direction.Axis.Y && (half == DoubleBlockHalf.LOWER) == (direction == Direction.UP)) {
            // The other half: it must still be this pump's other half.
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

    /** Breaking the top half breaks the bottom one properly, so the pump (and its fuel) drops from there. */
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

    /** The pump's block entity, from either half. */
    @Nullable
    public static GasPumpBlockEntity pumpAt(BlockGetter level, BlockPos pos, BlockState state) {
        BlockPos lower = state.getValue(HALF) == DoubleBlockHalf.LOWER ? pos : pos.below();
        return level.getBlockEntity(lower) instanceof GasPumpBlockEntity pump ? pump : null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        GasPumpBlockEntity pump = pumpAt(level, pos, state);
        if (pump == null) {
            return InteractionResult.PASS;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            PumpMenu.open(serverPlayer, pump);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** A fuel bucket (or tank item) from the owner tops the pump up; anything else opens the screen. */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (FluidUtil.getFluidContained(stack).isEmpty()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        GasPumpBlockEntity pump = pumpAt(level, pos, state);
        if (pump == null) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide) {
            if (!pump.canManage(player)) {
                player.displayClientMessage(Component.translatable("pump.reikocars.owner_only_fill", pump.ownerName()), true);
            } else if (!FluidUtil.interactWithFluidHandler(player, hand, pump.tank())) {
                player.displayClientMessage(Component.translatable("pump.reikocars.wrong_fluid", fuel.displayName()), true);
            }
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    // ---- block entity ----

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER ? new GasPumpBlockEntity(pos, state) : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != ModBlockEntities.GAS_PUMP.get()) {
            return null;
        }
        return (tickLevel, pos, tickState, blockEntity) -> ((GasPumpBlockEntity) blockEntity).serverTick();
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}
