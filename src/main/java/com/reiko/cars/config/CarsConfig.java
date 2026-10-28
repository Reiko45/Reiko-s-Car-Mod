package com.reiko.cars.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Server settings, in {@code serverconfig/reikocars-server.toml} inside the world folder. NeoForge sends them to
 * players when they join, so the driving settings below also apply to the client that steers the car.
 */
public final class CarsConfig {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.DoubleValue FUEL_USE;
    public static final ModConfigSpec.DoubleValue IDLE_FUEL_USE;
    public static final ModConfigSpec.BooleanValue BUCKET_FUELING;
    public static final ModConfigSpec.DoubleValue FALLBACK_FUEL_ENERGY;

    public static final ModConfigSpec.DoubleValue SPEED;
    public static final ModConfigSpec.DoubleValue OFF_ROAD_SPEED;
    public static final ModConfigSpec.DoubleValue RUN_OVER_DAMAGE;
    public static final ModConfigSpec.BooleanValue LOCK_NEW_CARS;

    public static final ModConfigSpec.DoubleValue DEFAULT_PUMP_PRICE;
    public static final ModConfigSpec.IntValue PUMP_CAPACITY;
    public static final ModConfigSpec.DoubleValue HOSE_LENGTH;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.push("fuel");
        FUEL_USE = builder
                .comment("Multiplies how much fuel cars burn per block driven. 0.5 doubles every car's range; 0 makes driving free.")
                .defineInRange("consumptionMultiplier", 1.0, 0.0, 100.0);
        IDLE_FUEL_USE = builder
                .comment("MJ burned every tick while someone sits in the driver's seat, even standing still. A bucket of light fuel holds 90,000 MJ.")
                .defineInRange("idleBurnPerTick", 0.5, 0.0, 10_000.0);
        BUCKET_FUELING = builder
                .comment("Lets players pour fuel into a car straight from a bucket or tank item, without a gas pump.")
                .define("allowBucketFueling", true);
        FALLBACK_FUEL_ENERGY = builder
                .comment("MJ per bucket for a fluid in the reikocars:car_fuels tag that BuildCraft has no fuel value for.")
                .defineInRange("fallbackEnergyPerBucket", 90_000.0, 1.0, 100_000_000.0);
        builder.pop();

        builder.push("driving");
        SPEED = builder
                .comment("Multiplies every car's top speed and acceleration.")
                .defineInRange("speedMultiplier", 1.0, 0.1, 3.0);
        OFF_ROAD_SPEED = builder
                .comment("Top speed on grass, dirt, sand, snow and the other blocks in the reikocars:off_road tag, as a fraction",
                        "of full speed. The off-roader ignores this. Set to 1.0 to drive everywhere at full speed.")
                .defineInRange("offRoadSpeedFactor", 0.6, 0.05, 1.0);
        RUN_OVER_DAMAGE = builder
                .comment("Damage a car does to a mob it hits, per block per tick of speed (a sedan at full speed goes about 0.9).",
                        "Players are only hurt when PvP is on. 0 turns it off.")
                .defineInRange("runOverDamage", 6.0, 0.0, 100.0);
        LOCK_NEW_CARS = builder
                .comment("Whether a newly placed car starts locked, so only its owner can get in until they unlock it.")
                .define("lockNewCars", true);
        builder.pop();

        builder.push("pumps");
        DEFAULT_PUMP_PRICE = builder
                .comment("Price per bucket a newly placed gas pump starts at. The owner can change it.")
                .defineInRange("defaultPricePerBucket", 4.0, 0.0, 1_000_000.0);
        PUMP_CAPACITY = builder
                .comment("Buckets of fuel a gas pump holds.")
                .defineInRange("capacityBuckets", 16, 1, 1000);
        HOSE_LENGTH = builder
                .comment("How close, in blocks, a car must be to a pump to fill up.")
                .defineInRange("hoseLength", 6.0, 1.0, 32.0);
        builder.pop();

        SPEC = builder.build();
    }

    private CarsConfig() {
    }

    /** A setting's value, or its default before the server config has loaded (in menus, or on a client not yet in a world). */
    public static double get(ModConfigSpec.DoubleValue value) {
        return SPEC.isLoaded() ? value.get() : value.getDefault();
    }

    public static boolean get(ModConfigSpec.BooleanValue value) {
        return SPEC.isLoaded() ? value.get() : value.getDefault();
    }

    public static int get(ModConfigSpec.IntValue value) {
        return SPEC.isLoaded() ? value.get() : value.getDefault();
    }
}
