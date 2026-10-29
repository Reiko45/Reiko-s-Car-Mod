package com.reiko.cars.repair;

import com.reiko.cars.car.CarCondition;
import com.reiko.cars.car.CarCondition.Part;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;

/**
 * Something that wears out and can be fixed at a repair bench: a car (every part) or a trailer (tires, brakes and
 * body: it has no engine).
 */
public interface Repairable {
    /** Its wear and tear (exact on the server). */
    CarCondition condition();

    /** Sets its wear and tear. Server side. */
    void setCondition(CarCondition condition);

    /** Whether it has {@code part} at all. */
    boolean hasPart(Part part);

    /** How many tires a new set is. */
    int wheelCount();

    /** Diesel trucks and trailers take twice the oil and brake pads, and twice the engine parts and body panels. */
    boolean isHeavy();

    /** The kind of vehicle, like "Sedan" or "Box Trailer", for the repair bench screen. */
    Component kindName();

    default Entity entity() {
        return (Entity) this;
    }
}
