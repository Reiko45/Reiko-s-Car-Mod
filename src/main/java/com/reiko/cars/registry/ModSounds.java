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
    /** Worn brakes grinding. */
    public static final Supplier<SoundEvent> BRAKE_SQUEAL = register("car.brake_squeal");
    /** The engine sputtering out when a part gives up. */
    public static final Supplier<SoundEvent> BREAKDOWN = register("car.breakdown");
    /** Spanners at work, as parts go on at a repair bench. */
    public static final Supplier<SoundEvent> REPAIR = register("repair_bench.repair");
    /** A police car's or ambulance's siren: a wail, looped. */
    public static final Supplier<SoundEvent> SIREN = register("car.siren");
    /** The ice cream truck's jingle (Greensleeves). */
    public static final Supplier<SoundEvent> ICE_CREAM_JINGLE = register("car.ice_cream_jingle");
    /** The dump truck's bed going up or down. */
    public static final Supplier<SoundEvent> HYDRAULIC = register("car.hydraulic");
    /** The garbage truck's packer crushing a load. */
    public static final Supplier<SoundEvent> COMPACTOR = register("car.compactor");
    public static final Supplier<SoundEvent> SIGNAL_TICK = register("car.signal_tick");
    public static final Supplier<SoundEvent> SIGNAL_TOCK = register("car.signal_tock");
    public static final Supplier<SoundEvent> ALARM = register("car.alarm");
    /** An electric car's motor: a whine, looped, rising with speed. */
    public static final Supplier<SoundEvent> ELECTRIC_MOTOR = register("car.electric_motor");
    /** An electric car's chime as it comes on, where a fuel car's starter would turn. */
    public static final Supplier<SoundEvent> EV_READY = register("car.ev_ready");
    /** A charging station's connector going in and the power coming up. */
    public static final Supplier<SoundEvent> CHARGE = register("charging_station.charge");

    private ModSounds() {
    }

    static {
        // the radio's tracks: radio.<track>, looked up by name when they play
        for (com.reiko.cars.radio.RadioStation station : com.reiko.cars.radio.RadioStation.values()) {
            for (String track : station.tracks()) {
                register("radio." + track);
            }
        }
    }

    private static Supplier<SoundEvent> register(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(ReikoCars.id(name)));
    }
}
