package com.reiko.cars.traffic;

import com.mojang.serialization.MapCodec;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * A speed limit sign on its post. Its owner sets the limit, and its speed camera fines drivers going over it within
 * {@link TrafficCameras#SPEED_REACH} blocks.
 */
public final class SpeedSignBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final MapCodec<SpeedSignBlock> CODEC = simpleCodec(SpeedSignBlock::new);
    private static final VoxelShape SHAPE_NS = Shapes.or(Block.box(7, 0, 7, 9, 16, 9), Block.box(2, 3, 6, 14, 16, 10));
    private static final VoxelShape SHAPE_EW = Shapes.or(Block.box(7, 0, 7, 9, 16, 9), Block.box(6, 3, 2, 10, 16, 14));

    public SpeedSignBlock(Properties properties) {
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
        return RoadShapes.rotate(state.getValue(FACING), SHAPE_NS, SHAPE_EW);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        if (!level.isClientSide && placer instanceof Player player && level.getBlockEntity(pos) instanceof SpeedSignBlockEntity sign) {
            sign.setOwner(player);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof SpeedSignBlockEntity sign)) {
            return InteractionResult.PASS;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            if (sign.isOwner(player)) {
                RoadSignMenu.open(serverPlayer, sign);
            } else {
                Component limit = Component.translatable(sign.kmh() ? "traffic.reikocars.kmh" : "traffic.reikocars.mph", sign.limit());
                serverPlayer.displayClientMessage(sign.fine() > 0
                        ? Component.translatable("traffic.reikocars.speed_camera", limit, sign.ownerName(), EconomyApi.get().format(sign.fine()))
                        : Component.translatable("traffic.reikocars.speed_no_camera", limit), true);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SpeedSignBlockEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}
