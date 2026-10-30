package com.reiko.cars.tuning;

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
    FUEL_TANK;

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
            case BRAKES -> current.brakes() >= Upgrades.MAX_BRAKES ? null : current.withBrakes(current.brakes() + 1);
            case LOWERING -> current.suspension() == Upgrades.Suspension.LOWERED ? null : current.withSuspension(Upgrades.Suspension.LOWERED);
            case LIFT -> current.suspension() == Upgrades.Suspension.LIFTED ? null : current.withSuspension(Upgrades.Suspension.LIFTED);
            case FUEL_TANK -> current.tank() >= Upgrades.MAX_TANK ? null : current.withTank(current.tank() + 1);
        };
    }

    /** The kit that comes off when this one goes on over it (one set of tires or springs swapped for another), or null. */
    @Nullable
    public TuningKit replaces(Upgrades current) {
        return switch (this) {
            case SPORT_TIRES -> current.tires() == Upgrades.Tires.OFF_ROAD ? OFF_ROAD_TIRES : null;
            case OFF_ROAD_TIRES -> current.tires() == Upgrades.Tires.SPORT ? SPORT_TIRES : null;
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
            case FUEL_TANK -> current.tank() == 0 ? Component.translatable("tuning.reikocars.state.stock")
                    : Component.translatable("tuning.reikocars.state.stage", current.tank(), Upgrades.MAX_TANK);
            default -> fit(current) == null ? Component.translatable("tuning.reikocars.state.fitted")
                    : Component.translatable("tuning.reikocars.state.none");
        };
    }

    /** Whether a {@code type} can take this kit at all: no fuel tank in an electric car. */
    public boolean suits(com.reiko.cars.car.CarType type) {
        return this != FUEL_TANK || !type.isElectric();
    }

    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }
}
