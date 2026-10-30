package com.reiko.cars.fire;

import com.reiko.cars.car.CarEntity;
import com.reiko.cars.config.CarsConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidActionResult;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * The fire truck's water. Its tank (config {@code fireTruck.waterTankBuckets}) feeds the fire hose ({@link
 * Firefighting}). It fills three ways: parked by a fire hydrant ({@link FireHydrantBlockEntity}), fastest; parked
 * beside open water (a lake, a river, a pool), which its pump draws from, a bucket a second; or by the bucket, poured
 * in by hand.
 */
public final class FireTrucks {
    /** Millibuckets a tick the pump draws from open water. */
    public static final int DRAFT_RATE = 50;
    /** How often it checks for water to draw from, in ticks. */
    static final int DRAFT_EVERY = 10;
    /** How far out from the truck's sides its suction hose reaches for open water, in blocks. */
    static final double DRAFT_REACH = 3.0;

    private FireTrucks() {
    }

    /** Millibuckets a full tank holds. */
    public static int tankCapacity() {
        return CarsConfig.get(CarsConfig.WATER_TANK) * 1000;
    }

    /** On the server, every tick. */
    public static void serverTick(CarEntity truck, long now) {
        FireLadder.tick(truck, now);
        if (now % DRAFT_EVERY == 0 && truck.water() < tankCapacity() && truck.standingStill()) {
            BlockPos water = openWater(truck.level(), truck);
            if (water != null) {
                truck.setWater(truck.water() + DRAFT_RATE * DRAFT_EVERY);
                if (truck.level() instanceof ServerLevel level) {
                    level.sendParticles(ParticleTypes.SPLASH, water.getX() + 0.5, water.getY() + 1.0, water.getZ() + 0.5, 6, 0.3, 0.05, 0.3, 0.1);
                    if (now % (DRAFT_EVERY * 4) == 0) {
                        level.playSound(null, water, SoundEvents.BUCKET_FILL, SoundSource.NEUTRAL, 0.5F, 0.8F);
                    }
                }
            }
        }
    }

    /** A block of still water (a source) its pump can reach, or null: beside it, or under it, up to two blocks down. */
    public static BlockPos openWater(Level level, CarEntity truck) {
        AABB box = truck.getBoundingBox();
        int y0 = (int) Math.floor(box.minY) - 2;
        int y1 = (int) Math.floor(box.minY) + 1;
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        Vec3 middle = truck.position();
        for (BlockPos pos : BlockPos.betweenClosed((int) Math.floor(box.minX - DRAFT_REACH), y0, (int) Math.floor(box.minZ - DRAFT_REACH),
                (int) Math.floor(box.maxX + DRAFT_REACH), y1, (int) Math.floor(box.maxZ + DRAFT_REACH))) {
            FluidState fluid = level.getFluidState(pos);
            if (fluid.isSource() && fluid.is(FluidTags.WATER)) {
                double distance = pos.getCenter().distanceToSqr(middle);
                if (distance < bestDistance) {
                    bestDistance = distance;
                    best = pos.immutable();
                }
            }
        }
        return best;
    }

    /** Pours a bucket (or any container) of water from {@code player}'s hand into the tank. */
    public static void pourWater(CarEntity truck, Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        FluidActionResult result = FluidUtil.tryEmptyContainer(held, new WaterTank(truck), Integer.MAX_VALUE, player, true);
        if (result.isSuccess()) {
            player.setItemInHand(hand, ItemUtils.createFilledResult(held, player, result.getResult()));
            player.displayClientMessage(Component.translatable("fire.reikocars.water_level", truck.water() / 1000, tankCapacity() / 1000), true);
        } else {
            player.displayClientMessage(Component.translatable(truck.water() >= tankCapacity() ? "fire.reikocars.tank_full"
                    : "fire.reikocars.no_room_for_bucket"), true);
        }
    }

    /** A fire truck's water tank as a fluid handler: water in, nothing out. */
    public static final class WaterTank implements IFluidHandler {
        private final CarEntity truck;

        public WaterTank(CarEntity truck) {
            this.truck = truck;
        }

        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return truck.water() > 0 ? new FluidStack(net.minecraft.world.level.material.Fluids.WATER, truck.water()) : FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tank) {
            return tankCapacity();
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return stack.getFluid().is(FluidTags.WATER);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (resource.isEmpty() || !isFluidValid(0, resource)) {
                return 0;
            }
            int taken = Math.min(resource.getAmount(), tankCapacity() - truck.water());
            if (taken > 0 && action.execute()) {
                truck.setWater(truck.water() + taken);
            }
            return Math.max(0, taken);
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
