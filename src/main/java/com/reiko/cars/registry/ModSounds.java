package com.reiko.cars.registry;

import com.reiko.cars.ReikoCars;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, ReikoCars.MODID);

    /** A seamless loop; its pitch rises with the car's speed. */
    public static final Supplier<SoundEvent> ENGINE = register("car.engine");
    public static final Supplier<SoundEvent> ENGINE_START = register("car.engine_start");
    public static final Supplier<SoundEvent> HORN = register("car.horn");
    public static final Supplier<SoundEvent> LOCK = register("car.lock");
    public static final Supplier<SoundEvent> PUMP = register("gas_pump.pump");

    private ModSounds() {
    }

    private static Supplier<SoundEvent> register(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(ReikoCars.id(name)));
    }
}
