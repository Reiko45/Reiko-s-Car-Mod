package com.reiko.cars.registry;

import com.reiko.cars.ReikoCars;
import com.reiko.cars.car.CarCondition;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** How a car's wear and tear travels to clients with the rest of its entity data. */
public final class ModDataSerializers {
    public static final DeferredRegister<EntityDataSerializer<?>> SERIALIZERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.ENTITY_DATA_SERIALIZERS, ReikoCars.MODID);

    /** Made here, before any car is, since entity data ids are handed out when the car class loads. */
    public static final EntityDataSerializer<CarCondition> CONDITION = EntityDataSerializer.forValueType(CarCondition.STREAM_CODEC);

    static {
        SERIALIZERS.register("car_condition", () -> CONDITION);
    }

    private ModDataSerializers() {
    }
}
