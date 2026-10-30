package com.reiko.cars.tuning;

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
 * The Tuning Bench: a tuning shop's bench for upgrading cars parked beside it. Right-click it to fit your own tuning
 * kits for free, or have the bench's owner fit theirs from its stock for the prices they set, and to paint a race
 * number and stripes on. Whoever places it owns it, and only they (or an admin) can break it.
 */
public final class TuningBenchBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final MapCodec<TuningBenchBlock> CODEC = simpleCodec(TuningBenchBlock::new);
    private static final VoxelShape SHAPE = Shapes.or(Block.box(0, 0, 0, 16, 15, 16), Block.box(0, 15, 0, 16, 16, 16));

    public TuningBenchBlock(Properties properties) {
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
        if (!level.isClientSide && placer instanceof Player player && level.getBlockEntity(pos) instanceof TuningBenchBlockEntity bench) {
            bench.setOwner(player);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof TuningBenchBlockEntity bench)) {
            return InteractionResult.PASS;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            TuningBenchMenu.open(serverPlayer, bench);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** The kits in stock spill out when the bench is broken. */
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof TuningBenchBlockEntity bench) {
            bench.dropStock();
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TuningBenchBlockEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    /** Only its owner (or an admin) can break a Tuning Bench, so its stock is safe. */
    @net.neoforged.fml.common.EventBusSubscriber(modid = com.reiko.cars.ReikoCars.MODID)
    public static final class Protection {
        private Protection() {
        }

        @net.neoforged.bus.api.SubscribeEvent
        static void onBreak(net.neoforged.neoforge.event.level.BlockEvent.BreakEvent event) {
            if (event.getState().getBlock() instanceof TuningBenchBlock
                    && event.getLevel().getBlockEntity(event.getPos()) instanceof TuningBenchBlockEntity bench
                    && !bench.canManage(event.getPlayer())) {
                event.setCanceled(true);
                event.getPlayer().displayClientMessage(net.minecraft.network.chat.Component.translatable("tuning.reikocars.protected", bench.ownerName()),
                        true);
            }
        }
    }
}
