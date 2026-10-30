package com.reiko.cars.registry;

import com.mojang.serialization.Codec;
import com.reiko.cars.ReikoCars;
import com.reiko.cars.car.CarCondition;
import com.reiko.cars.key.CarKey;
import com.reiko.cars.mail.Parcel;
import java.util.function.Supplier;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Data items carry: a picked-up car's fuel and wear (its paint uses vanilla's dyed colour), a picked-up tanker's load, a
 * broken gas pump's fuel and price, so nothing is lost when they're moved, and what's in a parcel and who it's for.
 */
public final class ModDataComponents {
    public static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, ReikoCars.MODID);

    /** MJ of fuel in the car's tank. */
    public static final Supplier<DataComponentType<Double>> FUEL = COMPONENTS.registerComponentType("fuel",
            builder -> builder.persistent(Codec.DOUBLE).networkSynchronized(ByteBufCodecs.DOUBLE));

    /** Millibuckets of water in a fire truck's tank. */
    public static final Supplier<DataComponentType<Integer>> WATER = COMPONENTS.registerComponentType("water",
            builder -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    /** The fuel left in a gas pump when it was broken. */
    public static final Supplier<DataComponentType<SimpleFluidContent>> PUMP_FUEL = COMPONENTS.registerComponentType("pump_fuel",
            builder -> builder.persistent(SimpleFluidContent.CODEC).networkSynchronized(SimpleFluidContent.STREAM_CODEC));

    /** What was in a tanker trailer's tank when it was picked up. */
    public static final Supplier<DataComponentType<SimpleFluidContent>> TANK_FLUID = COMPONENTS.registerComponentType("tank_fluid",
            builder -> builder.persistent(SimpleFluidContent.CODEC).networkSynchronized(SimpleFluidContent.STREAM_CODEC));

    /** MJ stored in a charging station when it was broken. */
    public static final Supplier<DataComponentType<Long>> CHARGER_ENERGY = COMPONENTS.registerComponentType("charger_energy",
            builder -> builder.persistent(Codec.LONG).networkSynchronized(ByteBufCodecs.VAR_LONG));

    /** A gas pump's price per bucket, or a charging station's per 1,000 MJ, in cents. */
    public static final Supplier<DataComponentType<Long>> PUMP_PRICE = COMPONENTS.registerComponentType("pump_price",
            builder -> builder.persistent(Codec.LONG).networkSynchronized(ByteBufCodecs.VAR_LONG));

    /** A picked-up car's wear and tear. Missing means as new. */
    public static final Supplier<DataComponentType<CarCondition>> CONDITION = COMPONENTS.registerComponentType("condition",
            builder -> builder.persistent(CarCondition.CODEC).networkSynchronized(CarCondition.STREAM_CODEC));

    /** A parcel's addressee, sender, postage and contents. */
    public static final Supplier<DataComponentType<Parcel>> PARCEL = COMPONENTS.registerComponentType("parcel",
            builder -> builder.persistent(Parcel.CODEC).networkSynchronized(Parcel.STREAM_CODEC));

    /** What a cut key fits. */
    public static final Supplier<DataComponentType<CarKey>> CAR_KEY = COMPONENTS.registerComponentType("car_key",
            builder -> builder.persistent(CarKey.CODEC).networkSynchronized(CarKey.STREAM_CODEC));

    /** A picked-up car's or trailer's lock, so the keys cut for it still fit when it's put down again. */
    public static final Supplier<DataComponentType<Long>> LOCK = COMPONENTS.registerComponentType("lock",
            builder -> builder.persistent(Codec.LONG).networkSynchronized(ByteBufCodecs.VAR_LONG));

    /** A picked-up car's insurance, so its cover carries on when it's put down again. */
    public static final Supplier<DataComponentType<com.reiko.cars.insurance.Policy>> INSURANCE = COMPONENTS.registerComponentType("insurance",
            builder -> builder.persistent(com.reiko.cars.insurance.Policy.CODEC).networkSynchronized(com.reiko.cars.insurance.Policy.STREAM_CODEC));

    /** The marking a can of Road Paint is set to paint. */
    public static final Supplier<DataComponentType<com.reiko.cars.road.RoadMarking>> ROAD_MARKING = COMPONENTS.registerComponentType("road_marking",
            builder -> builder.persistent(com.reiko.cars.road.RoadMarking.CODEC).networkSynchronized(com.reiko.cars.road.RoadMarking.STREAM_CODEC));

    /** A picked-up car's license plate, so it keeps it when it's put down again. */
    public static final Supplier<DataComponentType<String>> PLATE = COMPONENTS.registerComponentType("plate",
            builder -> builder.persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8));

    /** A driver's license card: whose, its number, and its points and suspension when last looked at. */
    public static final Supplier<DataComponentType<com.reiko.cars.license.LicenseCard>> LICENSE = COMPONENTS.registerComponentType("license",
            builder -> builder.persistent(com.reiko.cars.license.LicenseCard.CODEC).networkSynchronized(com.reiko.cars.license.LicenseCard.STREAM_CODEC));

    /** A picked-up car's tuning, so it keeps it. */
    public static final Supplier<DataComponentType<com.reiko.cars.tuning.Upgrades>> UPGRADES = COMPONENTS.registerComponentType("upgrades",
            builder -> builder.persistent(com.reiko.cars.tuning.Upgrades.CODEC).networkSynchronized(com.reiko.cars.tuning.Upgrades.STREAM_CODEC));

    /** A picked-up car's race number and stripes. */
    public static final Supplier<DataComponentType<com.reiko.cars.tuning.Livery>> LIVERY = COMPONENTS.registerComponentType("livery",
            builder -> builder.persistent(com.reiko.cars.tuning.Livery.CODEC).networkSynchronized(com.reiko.cars.tuning.Livery.STREAM_CODEC));

    /** A picked-up car's dirt, 0 (clean) to 1 (filthy). */
    public static final Supplier<DataComponentType<Float>> DIRT = COMPONENTS.registerComponentType("dirt",
            builder -> builder.persistent(com.mojang.serialization.Codec.floatRange(0.0F, 1.0F))
                    .networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.FLOAT));

    private ModDataComponents() {
    }
}
