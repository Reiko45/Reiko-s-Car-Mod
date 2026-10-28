package com.reiko.cars.registry;

import com.mojang.serialization.Codec;
import com.reiko.cars.ReikoCars;
import java.util.function.Supplier;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Data items carry: a picked-up car's fuel (its paint uses vanilla's dyed colour), and a broken gas pump's fuel and
 * price, so nothing is lost when a pump is moved.
 */
public final class ModDataComponents {
    public static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, ReikoCars.MODID);

    /** MJ of fuel in the car's tank. */
    public static final Supplier<DataComponentType<Double>> FUEL = COMPONENTS.registerComponentType("fuel",
            builder -> builder.persistent(Codec.DOUBLE).networkSynchronized(ByteBufCodecs.DOUBLE));

    /** The fuel left in a gas pump when it was broken. */
    public static final Supplier<DataComponentType<SimpleFluidContent>> PUMP_FUEL = COMPONENTS.registerComponentType("pump_fuel",
            builder -> builder.persistent(SimpleFluidContent.CODEC).networkSynchronized(SimpleFluidContent.STREAM_CODEC));

    /** A gas pump's price per bucket, in cents. */
    public static final Supplier<DataComponentType<Long>> PUMP_PRICE = COMPONENTS.registerComponentType("pump_price",
            builder -> builder.persistent(Codec.LONG).networkSynchronized(ByteBufCodecs.VAR_LONG));

    private ModDataComponents() {
    }
}
