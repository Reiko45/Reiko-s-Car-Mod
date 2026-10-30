package com.reiko.cars.traffic;

import com.mojang.serialization.MapCodec;
import com.reiko.cars.config.CarsConfig;
import com.reiko.cars.registry.ModBlockEntities;
import com.reiko.economy.api.EconomyApi;
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
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * A traffic light's head, to put up on a Street Pole facing the traffic it's for. Every light runs on the same clock:
 * green, yellow, then red, with the lights facing north or south out of step with those facing east or west, so the
 * lights at a crossing take turns. A redstone signal holds a light on red. Its owner can fine drivers who run its red.
 */
public final class TrafficLightBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final MapCodec<TrafficLightBlock> CODEC = simpleCodec(TrafficLightBlock::new);
    public static final EnumProperty<Signal> SIGNAL = EnumProperty.create("signal", Signal.class);
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    private static final VoxelShape SHAPE_NS = Block.box(4.5, 0, 5, 11.5, 16, 11);
    private static final VoxelShape SHAPE_EW = Block.box(5, 0, 4.5, 11, 16, 11.5);

    public TrafficLightBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(SIGNAL, Signal.RED).setValue(POWERED, false));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, SIGNAL, POWERED);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(POWERED, context.getLevel().hasNeighborSignal(context.getClickedPos()));
        return state.setValue(SIGNAL, signal(context.getLevel(), state));
    }

    /** What a light in {@code state} shows now. */
    public static Signal signal(Level level, BlockState state) {
        return signalAt(level.getGameTime(), state.getValue(FACING), state.getValue(POWERED));
    }

    /** What a light facing {@code facing} shows at {@code gameTime}: on red while {@code powered}. */
    public static Signal signalAt(long gameTime, Direction facing, boolean powered) {
        if (powered) {
            return Signal.RED;
        }
        long green = Math.round(CarsConfig.get(CarsConfig.GREEN_SECONDS) * 20);
        long yellow = Math.round(CarsConfig.get(CarsConfig.YELLOW_SECONDS) * 20);
        long allRed = Math.round(CarsConfig.get(CarsConfig.ALL_RED_SECONDS) * 20);
        long half = green + yellow + allRed;
        long t = Math.floorMod(gameTime + (facing.getAxis() == Direction.Axis.X ? half : 0), 2 * half);
        return t < green ? Signal.GREEN : t < green + yellow ? Signal.YELLOW : Signal.RED;
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos from, boolean moving) {
        boolean powered = level.hasNeighborSignal(pos);
        if (powered != state.getValue(POWERED)) {
            BlockState next = state.setValue(POWERED, powered);
            level.setBlock(pos, next.setValue(SIGNAL, signal(level, next)), 3);
        }
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(FACING).getAxis() == Direction.Axis.Z ? SHAPE_NS : SHAPE_EW;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        if (!level.isClientSide && placer instanceof Player player && level.getBlockEntity(pos) instanceof TrafficLightBlockEntity light) {
            light.setOwner(player);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof TrafficLightBlockEntity light)) {
            return InteractionResult.PASS;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            if (light.isOwner(player)) {
                RoadSignMenu.open(serverPlayer, light);
            } else {
                serverPlayer.displayClientMessage(light.fine() > 0
                        ? Component.translatable("traffic.reikocars.light_camera", light.ownerName(), EconomyApi.get().format(light.fine()))
                        : Component.translatable("traffic.reikocars.light_no_camera", light.ownerName()), true);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TrafficLightBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide || type != ModBlockEntities.TRAFFIC_LIGHT.get() ? null : (lvl, pos, st, be) -> {
            if (lvl.getGameTime() % 4 == 0) {
                Signal now = signal(lvl, st);
                if (now != st.getValue(SIGNAL)) {
                    lvl.setBlock(pos, st.setValue(SIGNAL, now), 3);
                }
            }
        };
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}
