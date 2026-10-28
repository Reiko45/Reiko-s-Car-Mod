package com.reiko.cars.car;

import buildcraftreborn.api.fuels.BuildcraftFuelRegistry;
import buildcraftreborn.api.fuels.IFuel;
import buildcraftreborn.api.mj.MjAPI;
import com.reiko.cars.config.CarsConfig;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Which fluids run a car and how far they go. Fuels are gasoline or diesel ({@link FuelType}), and each car runs on
 * one of them ({@link CarType#fuel()}). How much energy a bucket holds comes from BuildCraft Reborn's fuel registry,
 * the same numbers the combustion engine uses.
 */
public final class CarFuel {
    private CarFuel() {
    }

    /** Whether {@code stack} is a fuel of either kind. */
    public static boolean isFuel(FluidStack stack) {
        return FuelType.of(stack) != null;
    }

    /** Whether a car of {@code type} runs on {@code stack}. */
    public static boolean runs(CarType type, FluidStack stack) {
        return FuelType.of(stack) == type.fuel();
    }

    /** MJ in a bucket of {@code stack}'s fluid, or 0 if it isn't a fuel. Server side: BuildCraft's values are data. */
    public static double energyPerBucket(FluidStack stack) {
        if (!isFuel(stack)) {
            return 0;
        }
        IFuel fuel = BuildcraftFuelRegistry.fuel == null ? null : BuildcraftFuelRegistry.fuel.getFuel(stack);
        if (fuel == null) {
            return CarsConfig.get(CarsConfig.FALLBACK_FUEL_ENERGY);
        }
        return (double) fuel.getPowerPerCycle() / MjAPI.MJ * fuel.getTotalBurningTime();
    }

    /** MJ in {@code millibuckets} of {@code stack}'s fluid. */
    public static double energyOf(FluidStack stack, int millibuckets) {
        return energyPerBucket(stack) * millibuckets / 1000.0;
    }

    /** The most of {@code stack}'s fluid, in millibuckets, that fits in {@code roomMj} of free tank. */
    public static int millibucketsThatFit(FluidStack stack, double roomMj) {
        double perBucket = energyPerBucket(stack);
        if (perBucket <= 0 || roomMj <= 0) {
            return 0;
        }
        return (int) Math.min(Integer.MAX_VALUE, Math.floor(roomMj / perBucket * 1000.0 + 1.0E-6));
    }
}
