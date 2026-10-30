package com.reiko.cars.registry;

import com.reiko.cars.ReikoCars;
import com.reiko.cars.car.CarEntity;
import com.reiko.cars.car.CarType;
import com.reiko.cars.trailer.TrailerEntity;
import com.reiko.cars.trailer.TrailerType;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredRegister;

/** One entity type per kind of car and of trailer, so each gets its own model, spawn item and name. */
public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, ReikoCars.MODID);

    private static final Map<CarType, Supplier<EntityType<CarEntity>>> CARS = new EnumMap<>(CarType.class);
    private static final Map<TrailerType, Supplier<EntityType<TrailerEntity>>> TRAILERS = new EnumMap<>(TrailerType.class);

    static {
        for (CarType type : CarType.values()) {
            CARS.put(type, ENTITIES.register(type.id(), () -> EntityType.Builder
                    .<CarEntity>of((entityType, level) -> new CarEntity(entityType, level, type), MobCategory.MISC)
                    .sized(type.width, type.height)
                    .clientTrackingRange(10)
                    .build(ReikoCars.id(type.id()).toString())));
        }
        for (TrailerType type : TrailerType.values()) {
            TRAILERS.put(type, ENTITIES.register(type.id(), () -> EntityType.Builder
                    .<TrailerEntity>of((entityType, level) -> new TrailerEntity(entityType, level, type), MobCategory.MISC)
                    .sized(type.width, type.height)
                    .clientTrackingRange(10)
                    .build(ReikoCars.id(type.id()).toString())));
        }
    }

    /** The seat someone sitting on a bus shelter's bench rides. */
    public static final Supplier<EntityType<com.reiko.cars.bus.BenchSeatEntity>> BENCH_SEAT = ENTITIES.register("bench_seat",
            () -> EntityType.Builder.<com.reiko.cars.bus.BenchSeatEntity>of(com.reiko.cars.bus.BenchSeatEntity::new, MobCategory.MISC)
                    .sized(0.2F, 0.2F)
                    .noSummon()
                    .noSave()
                    .clientTrackingRange(8)
                    .build(ReikoCars.id("bench_seat").toString()));

    private ModEntities() {
    }

    public static EntityType<CarEntity> car(CarType type) {
        return CARS.get(type).get();
    }

    public static EntityType<TrailerEntity> trailer(TrailerType type) {
        return TRAILERS.get(type).get();
    }
}
