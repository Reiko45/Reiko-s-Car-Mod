package com.reiko.cars.car;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * A car's tank as a fluid handler, so buckets, BuildCraft tanks and other mods' fluid items can fill it. It only
 * takes car fuels, turns them into energy on the way in, and can't be drained.
 */
public final class CarTank implements IFluidHandler {
    private final CarEntity car;

    public CarTank(CarEntity car) {
        this.car = car;
    }

    @Override
    public int getTanks() {
        return 1;
    }

    /** The tank holds energy rather than a fluid, so it never reports one. */
    @Override
    public FluidStack getFluidInTank(int tank) {
        return FluidStack.EMPTY;
    }

    /** Room left, in buckets of light fuel. */
    @Override
    public int getTankCapacity(int tank) {
        return car.carType().tankBuckets * 1000;
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        return CarFuel.isFuel(stack);
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        if (!CarFuel.isFuel(resource) || car.level().isClientSide) {
            return 0;
        }
        int amount = Math.min(resource.getAmount(), CarFuel.millibucketsThatFit(resource, car.fuelRoom()));
        if (amount > 0 && action.execute()) {
            car.addFuel(CarFuel.energyOf(resource, amount));
        }
        return Math.max(0, amount);
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
