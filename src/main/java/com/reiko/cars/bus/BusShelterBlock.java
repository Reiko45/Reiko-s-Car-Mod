package com.reiko.cars.bus;

import com.mojang.serialization.MapCodec;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * A bus shelter: {@link #SLOTS} blocks wide and two high, a glass back and ends under a roof, with a bench along the
 * back, a route map on one end and a BUS sign over the front, which faces whoever puts it down (the road). It's put
 * down from its left end as they see it and reaches to their right. Each block is a part ({@link #SLOT} across and
 * {@link #LEVEL} up); the left end's lower block holds the stop's block entity. Sit on the bench (right-click it) to
 * wait for the bus.
 */
public final class BusShelterBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final MapCodec<BusShelterBlock> CODEC = simpleCodec(BusShelterBlock::new);
    public static final int SLOTS = 3;
    public static final IntegerProperty SLOT = IntegerProperty.create("slot", 0, SLOTS - 1);
    public static final IntegerProperty LEVEL = IntegerProperty.create("level", 0, 1);
    /** The bench's seat, in model pixels: its top and how far back its middle is. */
    static final double SEAT_TOP = 7.5;
    static final double SEAT_BACK = 11.75;

    /** Shapes by level, slot and facing (2D data value), built from boxes for a shelter facing north. */
    private static final VoxelShape[][][] SHAPES = new VoxelShape[2][SLOTS][4];

    static {
        for (int level = 0; level < 2; level++) {
            for (int slot = 0; slot < SLOTS; slot++) {
                List<double[]> boxes = new ArrayList<>();
                if (level == 0) {
                    boxes.add(new double[] {0, 0, 14, 16, 16, 16}); // the back
                    boxes.add(new double[] {0, 0, 9.5, 16, SEAT_TOP, 14}); // the bench
                } else {
                    boxes.add(new double[] {0, 0, 14, 16, 14, 16});
                    boxes.add(new double[] {0, 14, 0, 16, 16, 16}); // the roof
                }
                double top = level == 0 ? 16 : 14;
                if (slot == 0) {
                    boxes.add(new double[] {15, 0, 2, 16, top, 14}); // the right-hand end (east, facing north)
                }
                if (slot == SLOTS - 1) {
                    boxes.add(new double[] {0, 0, 2, 1, top, 14});
                }
                for (Direction facing : Direction.Plane.HORIZONTAL) {
                    VoxelShape shape = Shapes.empty();
                    for (double[] box : boxes) {
                        shape = Shapes.or(shape, turned(box, facing));
                    }
                    SHAPES[level][slot][facing.get2DDataValue()] = shape;
                }
            }
        }
    }

    public BusShelterBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(SLOT, 0).setValue(LEVEL, 0));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, SLOT, LEVEL);
    }

    /** {@code box} (pixels, for a shelter facing north) turned to face {@code facing}. */
    private static VoxelShape turned(double[] box, Direction facing) {
        double x0 = box[0];
        double z0 = box[2];
        double x1 = box[3];
        double z1 = box[5];
        double[] t = switch (facing) {
            case EAST -> new double[] {16 - z1, x0, 16 - z0, x1};
            case SOUTH -> new double[] {16 - x1, 16 - z1, 16 - x0, 16 - z0};
            case WEST -> new double[] {z0, 16 - x1, z1, 16 - x0};
            default -> new double[] {x0, z0, x1, z1};
        };
        return Block.box(t[0], box[1], t[1], t[2], box[4], t[3]);
    }

    /** Which way the shelter reaches from its first block. */
    public static Direction across(BlockState state) {
        return state.getValue(FACING).getCounterClockWise();
    }

    /** The first block's lower part, which holds the stop. */
    public static BlockPos mainPos(BlockState state, BlockPos pos) {
        return pos.relative(across(state), -state.getValue(SLOT)).below(state.getValue(LEVEL));
    }

    public static BlockPos partPos(BlockState state, BlockPos main, int slot, int level) {
        return main.relative(across(state), slot).above(level);
    }

    /** Where someone sitting on the bench in the lower block at {@code pos} sits. */
    public static Vec3 seat(BlockState state, BlockPos pos) {
        Direction back = state.getValue(FACING).getOpposite();
        double out = (SEAT_BACK - 8.0) / 16.0;
        return new Vec3(pos.getX() + 0.5 + back.getStepX() * out, pos.getY() + SEAT_TOP / 16.0, pos.getZ() + 0.5 + back.getStepZ() * out);
    }

    /** The space under the roof, where someone waiting stands (or sits). */
    public static AABB inside(BlockState state, BlockPos main) {
        return new AABB(main).minmax(new AABB(partPos(state, main, SLOTS - 1, 1)));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(LEVEL)][state.getValue(SLOT)][state.getValue(FACING).get2DDataValue()];
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
        Level level = context.getLevel();
        BlockPos main = context.getClickedPos();
        for (int slot = 0; slot < SLOTS; slot++) {
            for (int up = 0; up < 2; up++) {
                if (slot == 0 && up == 0) {
                    continue;
                }
                BlockPos at = partPos(state, main, slot, up);
                if (!level.getBlockState(at).canBeReplaced(context) || !level.getWorldBorder().isWithinBounds(at) || level.isOutsideBuildHeight(at)) {
                    if (!level.isClientSide && context.getPlayer() != null) {
                        context.getPlayer().displayClientMessage(Component.translatable("bus.reikocars.no_room"), true);
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
        for (int slot = 0; slot < SLOTS; slot++) {
            for (int up = 0; up < 2; up++) {
                if (slot != 0 || up != 0) {
                    level.setBlock(partPos(state, pos, slot, up), state.setValue(SLOT, slot).setValue(LEVEL, up), Block.UPDATE_ALL);
                }
            }
        }
        if (placer instanceof Player player && level.getBlockEntity(pos) instanceof BusStopBlockEntity stop) {
            stop.setOwner(player);
        }
    }

    /** Breaking any block of the shelter takes the rest with it (one Bus Shelter comes back). */
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !level.isClientSide) {
            BlockPos main = mainPos(state, pos);
            for (int slot = 0; slot < SLOTS; slot++) {
                for (int up = 0; up < 2; up++) {
                    BlockPos at = partPos(state, main, slot, up);
                    BlockState other = level.getBlockState(at);
                    if (!at.equals(pos) && other.is(this) && other.getValue(SLOT) == slot && other.getValue(LEVEL) == up
                            && other.getValue(FACING) == state.getValue(FACING)) {
                        level.setBlock(at, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
                    }
                }
            }
            if (level instanceof ServerLevel server) {
                BusStopRegistry.get(server.getServer()).remove(GlobalPos.of(level.dimension(), main));
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    /** Right-clicked: the bench (the lower blocks) seats you; its owner gets their screen (sneak to sit down yourself). */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        BlockPos main = mainPos(state, pos);
        if (!(level.getBlockEntity(main) instanceof BusStopBlockEntity stop)) {
            return InteractionResult.PASS;
        }
        if (player instanceof ServerPlayer server) {
            boolean sat = false;
            if (state.getValue(LEVEL) == 0 && !(stop.canManage(player) && stop.owner() != null && !player.isSecondaryUseActive())) {
                sat = BusService.sit(server, stop, state, pos);
            }
            if (!sat) {
                BusService.useStop(server, stop, false);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(SLOT) == 0 && state.getValue(LEVEL) == 0 ? new BusStopBlockEntity(pos, state) : null;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}
