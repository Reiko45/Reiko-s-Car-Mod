package com.reiko.cars.car;

import buildcraftreborn.api.fuels.BuildcraftFuelRegistry;
import buildcraftreborn.api.fuels.IFuel;
import buildcraftreborn.api.mj.MjAPI;
import com.reiko.cars.ReikoCars;
import com.reiko.cars.config.CarsConfig;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Which fluids run a car and how far they go. A fluid is a car fuel if it's in the {@code reikocars:car_fuels} tag
 * (BuildCraft's fuels and the common gasoline and diesel tags by default, but not crude oil). How much energy a
 * bucket holds comes from BuildCraft Reborn's fuel registry, the same numbers the combustion engine uses, so a
 * bucket of dense fuel takes a car four times as far as a bucket of light fuel.
 */
public final class CarFuel {
    public static final TagKey<Fluid> CAR_FUELS = FluidTags.create(ReikoCars.id("car_fuels"));

    private CarFuel() {
    }

    public static boolean isFuel(FluidStack stack) {
        return !stack.isEmpty() && stack.is(CAR_FUELS);
    }

    /** MJ in a bucket of {@code stack}'s fluid, or 0 if it isn't a car fuel. Server side: BuildCraft's values are data. */
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
