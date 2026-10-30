package com.reiko.cars.impound;

import com.mojang.serialization.MapCodec;
import com.reiko.cars.ReikoCars;
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
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import org.jetbrains.annotations.Nullable;

/**
 * An impound lot's office: a booth under an IMPOUND sign. The ground within {@code impound.lotRadius} blocks of it is
 * the lot. Tow trucks bring badly parked cars here, and repossessed cars are sent here; their owners right-click the
 * office to pay and get them back. Only its owner (or an admin) can break it.
 */
public final class ImpoundOfficeBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final MapCodec<ImpoundOfficeBlock> CODEC = simpleCodec(ImpoundOfficeBlock::new);
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 13, 16);

    public ImpoundOfficeBlock(Properties properties) {
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
        return SHAPE;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        if (!level.isClientSide && placer instanceof Player player && level.getBlockEntity(pos) instanceof ImpoundOfficeBlockEntity office) {
            office.setOwner(player);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof ImpoundOfficeBlockEntity office)) {
            return InteractionResult.PASS;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            ImpoundMenu.open(serverPlayer, office);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level instanceof ServerLevel server) {
            ImpoundRegistry.get(server.getServer()).remove(GlobalPos.of(server.dimension(), pos));
            ImpoundService.officeGone(server, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ImpoundOfficeBlockEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    /** Only its owner (or an admin) can break an impound office. */
    @EventBusSubscriber(modid = ReikoCars.MODID)
    public static final class Protection {
        private Protection() {
        }

        @SubscribeEvent
        static void onBreak(BlockEvent.BreakEvent event) {
            if (event.getState().getBlock() instanceof ImpoundOfficeBlock
                    && event.getLevel().getBlockEntity(event.getPos()) instanceof ImpoundOfficeBlockEntity office
                    && !office.canManage(event.getPlayer())) {
                event.setCanceled(true);
                event.getPlayer().displayClientMessage(Component.translatable("impound.reikocars.protected", office.ownerName()), true);
            }
        }
    }
}
