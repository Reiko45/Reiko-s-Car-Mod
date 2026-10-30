package com.reiko.cars.wash;

import com.mojang.serialization.MapCodec;
import com.reiko.cars.car.CarEntity;
import com.reiko.cars.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * The drive-through Car Wash: an arch over {@link #LANE} blocks of road, two posts {@link #POSTS} blocks high and a
 * CAR WASH beam across the top, with spinning brushes and a spray bar (drawn by its renderer) that wash a car driven
 * slowly through it. It's put down from its left-hand post (the one with the pay terminal), facing whoever puts it down;
 * the lane runs toward them and away, and the arch reaches to their right. Each block of the arch is a part (its
 * {@link #SPAN} across from the left post and {@link #LEVEL} up); the left post's foot holds the block entity.
 */
public final class CarWashBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final MapCodec<CarWashBlock> CODEC = simpleCodec(CarWashBlock::new);
    /** Blocks of road between the posts. */
    public static final int LANE = 4;
    /** Blocks across, posts and all, and how high the posts stand under the beam. */
    public static final int SPANS = LANE + 2;
    public static final int POSTS = 4;
    public static final IntegerProperty SPAN = IntegerProperty.create("span", 0, SPANS - 1);
    public static final IntegerProperty LEVEL = IntegerProperty.create("level", 0, POSTS);
    /** On the left post's foot: whether it's washing a car (its brushes turn and it sprays). */
    public static final BooleanProperty RUNNING = BooleanProperty.create("running");

    private static final VoxelShape POST = Block.box(5, 0, 5, 11, 16, 11);
    private static final VoxelShape FOOT = Block.box(3, 0, 3, 13, 1, 13);
    private static final VoxelShape[] TERMINAL = rotations(Block.box(4, 6, 1, 12, 14, 5));
    private static final VoxelShape BEAM_X = Block.box(0, 2, 3.5, 16, 16, 12.5);
    private static final VoxelShape BEAM_Z = Block.box(3.5, 2, 0, 12.5, 16, 16);

    public CarWashBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(SPAN, 0).setValue(LEVEL, 0).setValue(RUNNING, false));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, SPAN, LEVEL, RUNNING);
    }

    /** Which way the arch reaches across the road from its left post. */
    public static Direction across(BlockState state) {
        return state.getValue(FACING).getCounterClockWise();
    }

    /** Whether a block {@code span} across and {@code level} up is part of the arch: a post, or the beam. */
    public static boolean isPart(int span, int level) {
        return level == POSTS || (level < POSTS && (span == 0 || span == SPANS - 1));
    }

    /** The left post's foot, which the block at {@code pos} belongs to. */
    public static BlockPos mainPos(BlockState state, BlockPos pos) {
        return pos.relative(across(state), -state.getValue(SPAN)).below(state.getValue(LEVEL));
    }

    /** Where the part {@code span} across and {@code level} up stands, for the arch whose left post's foot is {@code main}. */
    public static BlockPos partPos(BlockState state, BlockPos main, int span, int level) {
        return main.relative(across(state), span).above(level);
    }

    /**
     * The lane under the arch where cars get washed: between the posts, up to the beam, and a block and a half either
     * side of the arch along the road.
     */
    public static AABB lane(BlockState state, BlockPos main) {
        Direction across = across(state);
        Direction along = state.getValue(FACING);
        BlockPos first = main.relative(across, 1);
        BlockPos last = main.relative(across, LANE).above(POSTS - 1);
        AABB box = new AABB(first).minmax(new AABB(last));
        double reach = 1.5;
        return box.expandTowards(along.getStepX() * reach, 0.0, along.getStepZ() * reach)
                .expandTowards(-along.getStepX() * reach, 0.0, -along.getStepZ() * reach);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int span = state.getValue(SPAN);
        int up = state.getValue(LEVEL);
        if (up == POSTS) {
            return across(state).getAxis() == Direction.Axis.X ? BEAM_X : BEAM_Z;
        }
        if (up == 0) {
            return span == 0 ? Shapes.or(POST, FOOT, TERMINAL[state.getValue(FACING).get2DDataValue()]) : Shapes.or(POST, FOOT);
        }
        return POST;
    }

    /** {@code north} (a shape for a block facing north) turned for each facing, by its 2D data value. */
    private static VoxelShape[] rotations(VoxelShape north) {
        VoxelShape[] out = new VoxelShape[4];
        AABB box = north.bounds();
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            double x0 = box.minX * 16;
            double z0 = box.minZ * 16;
            double x1 = box.maxX * 16;
            double z1 = box.maxZ * 16;
            double[] turned = switch (facing) {
                case EAST -> new double[] {16 - z1, x0, 16 - z0, x1};
                case SOUTH -> new double[] {16 - x1, 16 - z1, 16 - x0, 16 - z0};
                case WEST -> new double[] {z0, 16 - x1, z1, 16 - x0};
                default -> new double[] {x0, z0, x1, z1};
            };
            out[facing.get2DDataValue()] = Block.box(turned[0], box.minY * 16, turned[1], turned[2], box.maxY * 16, turned[3]);
        }
        return out;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        BlockState state = defaultBlockState().setValue(FACING, facing);
        Level level = context.getLevel();
        BlockPos main = context.getClickedPos();
        for (int span = 0; span < SPANS; span++) {
            for (int up = 0; up <= POSTS; up++) {
                if (!isPart(span, up) || (span == 0 && up == 0)) {
                    continue;
                }
                BlockPos at = partPos(state, main, span, up);
                if (!level.getBlockState(at).canBeReplaced(context) || !level.getWorldBorder().isWithinBounds(at) || level.isOutsideBuildHeight(at)) {
                    if (!level.isClientSide && context.getPlayer() != null) {
                        context.getPlayer().displayClientMessage(Component.translatable("car_wash.reikocars.no_room"), true);
                    }
                    return null;
                }
            }
        }
        return state;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        if (level.isClientSide) {
            return;
        }
        for (int span = 0; span < SPANS; span++) {
            for (int up = 0; up <= POSTS; up++) {
                if (isPart(span, up) && !(span == 0 && up == 0)) {
                    level.setBlock(partPos(state, pos, span, up), state.setValue(SPAN, span).setValue(LEVEL, up), Block.UPDATE_ALL);
                }
            }
        }
        if (placer instanceof Player player && level.getBlockEntity(pos) instanceof CarWashBlockEntity wash) {
            wash.setOwner(player);
        }
    }

    /** Breaking any block of the arch takes the rest with it (one Car Wash comes back). */
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !level.isClientSide) {
            BlockPos main = mainPos(state, pos);
            for (int span = 0; span < SPANS; span++) {
                for (int up = 0; up <= POSTS; up++) {
                    if (!isPart(span, up)) {
                        continue;
                    }
                    BlockPos at = partPos(state, main, span, up);
                    BlockState other = level.getBlockState(at);
                    if (!at.equals(pos) && other.is(this) && other.getValue(SPAN) == span && other.getValue(LEVEL) == up
                            && other.getValue(FACING) == state.getValue(FACING)) {
                        level.setBlock(at, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
                    }
                }
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    /**
     * Right-clicked: from the driver's seat, paying for a wash (as the hitch key does); on foot, the owner's screen, or
     * how to pay.
     */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        BlockPos main = mainPos(state, pos);
        if (!(level.getBlockEntity(main) instanceof CarWashBlockEntity wash)) {
            return InteractionResult.PASS;
        }
        if (player instanceof ServerPlayer server) {
            if (player.getVehicle() instanceof CarEntity car && car.getControllingPassenger() == player) {
                CarWashService.offer(server, car, wash);
            } else if (wash.canManage(player) && wash.owner() != null) {
                WashOwnerMenu.open(server, wash, WashOwnerMenu.TUNNEL);
            } else {
                server.displayClientMessage(Component.translatable("car_wash.reikocars.how_to_pay", CarWashService.priceText(wash)), true);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(SPAN) == 0 && state.getValue(LEVEL) == 0 ? new CarWashBlockEntity(pos, state) : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type != ModBlockEntities.CAR_WASH.get()) {
            return null;
        }
        return level.isClientSide ? (tickLevel, pos, tickState, entity) -> ((CarWashBlockEntity) entity).clientTick()
                : (tickLevel, pos, tickState, entity) -> CarWashService.tick((CarWashBlockEntity) entity);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}
