package com.reiko.cars.registry;

import com.reiko.cars.ReikoCars;
import com.reiko.cars.car.CarEntity;
import com.reiko.cars.car.CarType;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredRegister;

/** One entity type per kind of car, so each gets its own model, spawn item and name. */
public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, ReikoCars.MODID);

    private static final Map<CarType, Supplier<EntityType<CarEntity>>> CARS = new EnumMap<>(CarType.class);

    static {
        for (CarType type : CarType.values()) {
            CARS.put(type, ENTITIES.register(type.id(), () -> EntityType.Builder
                    .<CarEntity>of((entityType, level) -> new CarEntity(entityType, level, type), MobCategory.MISC)
                    .sized(type.width, type.height)
                    .clientTrackingRange(10)
                    .build(ReikoCars.id(type.id()).toString())));
        }
    }

    private ModEntities() {
    }

    public static EntityType<CarEntity> car(CarType type) {
        return CARS.get(type).get();
    }
}
