package com.reiko.cars.wash;

import com.reiko.cars.car.CarEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.neoforged.neoforge.fluids.FluidActionResult;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * Washing a car by hand: right-clicking it with a bucket of water (or any container of it) throws a bucketful over it,
 * which takes {@link Dirt#BUCKET_WASH} of its dirt off, so three buckets get a filthy car clean. Anyone may wash any car.
 */
public final class HandWash {
    private static final int BUCKET = 1000;

    private HandWash() {
    }

    /** Whether {@code held} has water in it to wash with. */
    public static boolean holdsWater(ItemStack held) {
        return FluidUtil.getFluidContained(held).filter(fluid -> fluid.is(FluidTags.WATER) && fluid.getAmount() > 0).isPresent();
    }

    /** {@code player} throws the water in their hand over {@code car}. */
    public static InteractionResult wash(CarEntity car, Player player, InteractionHand hand) {
        if (car.level().isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (car.dirt() <= 0.0F && car.snow() <= 0.0F) {
            player.displayClientMessage(Component.translatable("dirt.reikocars.already_clean"), true);
            return InteractionResult.CONSUME;
        }
        ItemStack held = player.getItemInHand(hand);
        Sink sink = new Sink();
        FluidActionResult result = FluidUtil.tryEmptyContainer(held, sink, BUCKET, player, true);
        if (!result.isSuccess() || sink.filled <= 0) {
            return InteractionResult.PASS;
        }
        player.setItemInHand(hand, ItemUtils.createFilledResult(held, player, result.getResult()));
        car.setDirt(car.dirt() - Dirt.BUCKET_WASH * sink.filled / BUCKET);
        if (car.dirt() <= 0.0F && player instanceof net.minecraft.server.level.ServerPlayer washer) {
            com.reiko.cars.advancement.CarAdvancements.award(washer, "wash");
        }
        car.setSnow(car.snow() - Dirt.BUCKET_WASH * sink.filled / BUCKET);
        splash(car);
        player.displayClientMessage(car.dirt() <= 0.0F ? Component.translatable("dirt.reikocars.washed_clean")
                : Component.translatable("dirt.reikocars.washed", Dirt.describe(car.dirt())), true);
        return InteractionResult.CONSUME;
    }

    /** Water splashing off {@code car}. */
    public static void splash(CarEntity car) {
        if (!(car.level() instanceof ServerLevel level)) {
            return;
        }
        double w = car.carType().width * 0.4;
        double h = car.carType().height;
        level.sendParticles(ParticleTypes.SPLASH, car.getX(), car.getY() + h * 0.7, car.getZ(), 40, w, h * 0.3, w, 0.15);
        level.sendParticles(ParticleTypes.FALLING_WATER, car.getX(), car.getY() + h * 0.5, car.getZ(), 20, w, h * 0.3, w, 0.0);
        level.playSound(null, car.getX(), car.getY(), car.getZ(), SoundEvents.PLAYER_SPLASH, SoundSource.NEUTRAL, 0.5F, 1.2F);
    }

    /** Takes up to a bucket of water, and nothing else, so the container's emptied by what was thrown. */
    private static final class Sink implements IFluidHandler {
        int filled;

        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tank) {
            return BUCKET;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return stack.is(FluidTags.WATER);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (!resource.is(FluidTags.WATER)) {
                return 0;
            }
            int taken = Math.min(resource.getAmount(), BUCKET - filled);
            if (action.execute()) {
                filled += taken;
            }
            return taken;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return FluidStack.EMPTY;
        }
    }
}
