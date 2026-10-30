package com.reiko.cars.road;

import com.mojang.serialization.MapCodec;
import com.reiko.cars.ReikoCars;
import com.reiko.cars.dmv.ConeRegistry;
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
 * A traffic cone. Cars and people go through it rather than crash into it. Its owner right-clicks it to number it
 * (sneak to count down): numbered cones near a DMV of theirs are its driving test route, in number order. Only its
 * owner (or an admin) can pick it up.
 */
public final class TrafficConeBlock extends BaseEntityBlock {
    public static final MapCodec<TrafficConeBlock> CODEC = simpleCodec(TrafficConeBlock::new);
    public static final int MAX_NUMBER = 30;
    private static final VoxelShape SHAPE = Shapes.or(Block.box(2, 0, 2, 14, 1, 14), Block.box(4, 1, 4, 12, 7, 12), Block.box(6, 7, 6, 10, 15, 10));

    public TrafficConeBlock(Properties properties) {
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
        if (!level.isClientSide && placer instanceof Player player && level.getBlockEntity(pos) instanceof TrafficConeBlockEntity cone) {
            cone.setOwner(player);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof TrafficConeBlockEntity cone)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            if (cone.canManage(player)) {
                int number = Math.floorMod(cone.number() + (player.isShiftKeyDown() ? -1 : 1), MAX_NUMBER + 1);
                cone.setNumber(number);
                player.displayClientMessage(number == 0 ? Component.translatable("cone.reikocars.unnumbered")
                        : Component.translatable("cone.reikocars.numbered", number), true);
            } else if (cone.number() > 0) {
                player.displayClientMessage(Component.translatable("cone.reikocars.route_of", cone.number(), cone.ownerName()), true);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level instanceof ServerLevel server) {
            ConeRegistry.get(server.getServer()).remove(GlobalPos.of(server.dimension(), pos));
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TrafficConeBlockEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    /** Only its owner (or an admin) can pick up a numbered cone: the test route stays put. */
    @EventBusSubscriber(modid = ReikoCars.MODID)
    public static final class Protection {
        private Protection() {
        }

        @SubscribeEvent
        static void onBreak(BlockEvent.BreakEvent event) {
            if (event.getState().getBlock() instanceof TrafficConeBlock
                    && event.getLevel().getBlockEntity(event.getPos()) instanceof TrafficConeBlockEntity cone
                    && cone.number() > 0 && !cone.canManage(event.getPlayer())) {
                event.setCanceled(true);
                event.getPlayer().displayClientMessage(Component.translatable("cone.reikocars.protected", cone.ownerName()), true);
            }
        }
    }
}
