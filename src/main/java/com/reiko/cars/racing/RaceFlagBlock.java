package com.reiko.cars.racing;

import com.mojang.serialization.MapCodec;
import com.reiko.cars.ReikoCars;
import com.reiko.cars.racing.FlagRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import org.jetbrains.annotations.Nullable;

/**
 * A race flag: a checkered flag on a pole. Cars and people go through it. Its owner right-clicks it to number it (sneak
 * to count down): numbered flags near a Race Control of theirs are its track, in number order. Only its owner (or an
 * admin) can pick up a numbered one.
 */
public final class RaceFlagBlock extends BaseEntityBlock {
    public static final MapCodec<RaceFlagBlock> CODEC = simpleCodec(RaceFlagBlock::new);
    public static final int MAX_NUMBER = 30;
    private static final VoxelShape SHAPE = Shapes.or(Block.box(5, 0, 5, 11, 2, 11), Block.box(7, 2, 7, 9, 16, 9), Block.box(8, 9, 7.5, 16, 15, 8.5));

    public RaceFlagBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        if (!level.isClientSide && placer instanceof Player player && level.getBlockEntity(pos) instanceof RaceFlagBlockEntity flag) {
            flag.setOwner(player);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof RaceFlagBlockEntity flag)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            if (flag.canManage(player)) {
                int number = Math.floorMod(flag.number() + (player.isShiftKeyDown() ? -1 : 1), MAX_NUMBER + 1);
                flag.setNumber(number);
                player.displayClientMessage(number == 0 ? Component.translatable("flag.reikocars.unnumbered")
                        : Component.translatable("flag.reikocars.numbered", number), true);
            } else if (flag.number() > 0) {
                player.displayClientMessage(Component.translatable("flag.reikocars.route_of", flag.number(), flag.ownerName()), true);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level instanceof ServerLevel server) {
            FlagRegistry.get(server.getServer()).remove(GlobalPos.of(server.dimension(), pos));
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RaceFlagBlockEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    /** Only its owner (or an admin) can pick up a numbered flag: the track stays put. */
    @EventBusSubscriber(modid = ReikoCars.MODID)
    public static final class Protection {
        private Protection() {
        }

        @SubscribeEvent
        static void onBreak(BlockEvent.BreakEvent event) {
            if (event.getState().getBlock() instanceof RaceFlagBlock
                    && event.getLevel().getBlockEntity(event.getPos()) instanceof RaceFlagBlockEntity flag
                    && flag.number() > 0 && !flag.canManage(event.getPlayer())) {
                event.setCanceled(true);
                event.getPlayer().displayClientMessage(Component.translatable("flag.reikocars.protected", flag.ownerName()), true);
            }
        }
    }
}
