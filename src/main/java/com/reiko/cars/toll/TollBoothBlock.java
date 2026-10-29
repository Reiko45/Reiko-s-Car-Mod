package com.reiko.cars.toll;

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
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * A toll booth: its owner's toll gates within {@link TollService#BOOTH_REACH} blocks charge its toll. The owner
 * right-clicks it to set the toll and see the takings; anyone else is told the toll.
 */
public final class TollBoothBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final MapCodec<TollBoothBlock> CODEC = simpleCodec(TollBoothBlock::new);
    // the island, the kiosk and the sign on top; the sign runs across the block, so it turns with the booth
    private static final VoxelShape SHAPE_NS = net.minecraft.world.phys.shapes.Shapes.or(
            Block.box(0.5, 0, 0.5, 15.5, 2, 15.5), Block.box(2, 2, 3.5, 14, 14, 12.5), Block.box(2.5, 14, 6.5, 13.5, 19.5, 9.5));
    private static final VoxelShape SHAPE_EW = net.minecraft.world.phys.shapes.Shapes.or(
            Block.box(0.5, 0, 0.5, 15.5, 2, 15.5), Block.box(3.5, 2, 2, 12.5, 14, 14), Block.box(6.5, 14, 2.5, 9.5, 19.5, 13.5));

    public TollBoothBlock(Properties properties) {
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
        return state.getValue(FACING).getAxis() == net.minecraft.core.Direction.Axis.Z ? SHAPE_NS : SHAPE_EW;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        if (!level.isClientSide && placer instanceof Player player && level.getBlockEntity(pos) instanceof TollBoothBlockEntity booth) {
            booth.setOwner(player);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof TollBoothBlockEntity booth)) {
            return InteractionResult.PASS;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            if (booth.isOwner(player)) {
                TollBoothMenu.open(serverPlayer, booth);
            } else {
                serverPlayer.displayClientMessage(Component.translatable("toll.reikocars.booth_info", booth.ownerName(),
                        EconomyApi.get().format(booth.toll())), true);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TollBoothBlockEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}
