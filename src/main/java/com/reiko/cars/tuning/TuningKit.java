package com.reiko.cars.tuning;

import java.util.List;
import java.util.Locale;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/** The kits a Tuning Bench fits, each an item: what it does to a car, and what it takes off (to hand back). */
public enum TuningKit {
    ENGINE,
    TURBO,
    SPORT_TIRES,
    OFF_ROAD_TIRES,
    BRAKES,
    LOWERING,
    LIFT,
    FUEL_TANK,
    BATTERY,
    WINTER_TIRES;

    /** The order the Tuning Bench lists them in (saved prices go by {@link #ordinal()}, so new kits go on the end). */
    public static final List<TuningKit> ORDER = List.of(ENGINE, TURBO, SPORT_TIRES, OFF_ROAD_TIRES, WINTER_TIRES, BRAKES, LOWERING, LIFT, FUEL_TANK,
            BATTERY);

    public String id() {
        return switch (this) {
            case ENGINE -> "engine_tune_kit";
            case TURBO -> "turbocharger";
            case SPORT_TIRES -> "sport_tires";
            case OFF_ROAD_TIRES -> "off_road_tires";
            case BRAKES -> "performance_brakes";
            case LOWERING -> "lowering_springs";
            case LIFT -> "lift_kit";
            case FUEL_TANK -> "extended_fuel_tank";
            case BATTERY -> "battery_module";
            case WINTER_TIRES -> "winter_tires";
        };
    }

    public Component displayName() {
        return Component.translatable("item.reikocars." + id());
    }

    /** {@code current} with this kit fitted, or null if it can't take it (maxed out, or fitted already). */
    @Nullable
    public Upgrades fit(Upgrades current) {
        return switch (this) {
            case ENGINE -> current.engine() >= Upgrades.MAX_ENGINE ? null : current.withEngine(current.engine() + 1);
            case TURBO -> current.turbo() ? null : current.withTurbo(true);
            case SPORT_TIRES -> current.tires() == Upgrades.Tires.SPORT ? null : current.withTires(Upgrades.Tires.SPORT);
            case OFF_ROAD_TIRES -> current.tires() == Upgrades.Tires.OFF_ROAD ? null : current.withTires(Upgrades.Tires.OFF_ROAD);
            case WINTER_TIRES -> current.tires() == Upgrades.Tires.WINTER ? null : current.withTires(Upgrades.Tires.WINTER);
            case BRAKES -> current.brakes() >= Upgrades.MAX_BRAKES ? null : current.withBrakes(current.brakes() + 1);
            case LOWERING -> current.suspension() == Upgrades.Suspension.LOWERED ? null : current.withSuspension(Upgrades.Suspension.LOWERED);
            case LIFT -> current.suspension() == Upgrades.Suspension.LIFTED ? null : current.withSuspension(Upgrades.Suspension.LIFTED);
            case FUEL_TANK, BATTERY -> current.tank() >= Upgrades.MAX_TANK ? null : current.withTank(current.tank() + 1);
        };
    }

    /** The kit that comes off when this one goes on over it (one set of tires or springs swapped for another), or null. */
    @Nullable
    public TuningKit replaces(Upgrades current) {
        return switch (this) {
            case SPORT_TIRES, OFF_ROAD_TIRES, WINTER_TIRES -> {
                TuningKit fitted = tiresKit(current.tires());
                yield fitted == this ? null : fitted;
            }
            case LOWERING -> current.suspension() == Upgrades.Suspension.LIFTED ? LIFT : null;
            case LIFT -> current.suspension() == Upgrades.Suspension.LOWERED ? LOWERING : null;
            default -> null;
        };
    }

    /** How far {@code current} has this kit's upgrade: "2/3", "yes", "no", "stock". */
    public Component state(Upgrades current) {
        return switch (this) {
            case ENGINE -> current.engine() == 0 ? Component.translatable("tuning.reikocars.state.stock")
                    : Component.translatable("tuning.reikocars.state.stage", current.engine(), Upgrades.MAX_ENGINE);
            case BRAKES -> current.brakes() == 0 ? Component.translatable("tuning.reikocars.state.stock")
                    : Component.translatable("tuning.reikocars.state.stage", current.brakes(), Upgrades.MAX_BRAKES);
            case FUEL_TANK, BATTERY -> current.tank() == 0 ? Component.translatable("tuning.reikocars.state.stock")
                    : Component.translatable("tuning.reikocars.state.stage", current.tank(), Upgrades.MAX_TANK);
            default -> fit(current) == null ? Component.translatable("tuning.reikocars.state.fitted")
                    : Component.translatable("tuning.reikocars.state.none");
        };
    }

    /** Whether a {@code type} can take this kit at all: a fuel tank for gasoline and diesel cars, a battery for electric ones. */
    public boolean suits(com.reiko.cars.car.CarType type) {
        return switch (this) {
            case FUEL_TANK -> !type.isElectric();
            case BATTERY -> type.isElectric();
            default -> true;
        };
    }

    /** The kit for a set of tires, or null for the ones the car came with. */
    @Nullable
    public static TuningKit tiresKit(Upgrades.Tires tires) {
        return switch (tires) {
            case SPORT -> SPORT_TIRES;
            case OFF_ROAD -> OFF_ROAD_TIRES;
            case WINTER -> WINTER_TIRES;
            default -> null;
        };
    }

    /** Whether this is a set of tires (a car takes one set). */
    public boolean isTires() {
        return this == SPORT_TIRES || this == OFF_ROAD_TIRES || this == WINTER_TIRES;
    }

    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }
}
