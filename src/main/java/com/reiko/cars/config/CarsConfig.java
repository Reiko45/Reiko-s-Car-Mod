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
    public static final ModConfigSpec.DoubleValue ROAD_SPEED;
    public static final ModConfigSpec.DoubleValue RUN_OVER_DAMAGE;
    public static final ModConfigSpec.BooleanValue LOCK_NEW_CARS;
    public static final ModConfigSpec.BooleanValue REQUIRE_KEYS;
    public static final ModConfigSpec.IntValue ALARM_SECONDS;
    public static final ModConfigSpec.BooleanValue REAL_HEADLIGHTS;

    public static final ModConfigSpec.DoubleValue DEFAULT_PUMP_PRICE;
    public static final ModConfigSpec.IntValue PUMP_CAPACITY;
    public static final ModConfigSpec.DoubleValue HOSE_LENGTH;

    public static final ModConfigSpec.DoubleValue WEAR;
    public static final ModConfigSpec.DoubleValue CRASH_DAMAGE;
    public static final ModConfigSpec.BooleanValue BREAKDOWNS;
    public static final ModConfigSpec.DoubleValue BENCH_REACH;
    public static final ModConfigSpec.DoubleValue DEFAULT_PART_PRICE;

    public static final ModConfigSpec.DoubleValue BUS_FARE;
    public static final ModConfigSpec.DoubleValue TAXI_START;
    public static final ModConfigSpec.DoubleValue TAXI_PER_100;

    public static final ModConfigSpec.DoubleValue DEALERSHIP_REACH;

    public static final ModConfigSpec.DoubleValue PARKING_RATE;
    public static final ModConfigSpec.DoubleValue PARKING_FINE;
    public static final ModConfigSpec.IntValue PARKING_GRACE;
    public static final ModConfigSpec.IntValue PARKING_FINE_EVERY;
    public static final ModConfigSpec.DoubleValue TOLL;
    public static final ModConfigSpec.DoubleValue INSURANCE_PREMIUM;
    public static final ModConfigSpec.DoubleValue INSURANCE_DEDUCTIBLE;
    public static final ModConfigSpec.DoubleValue INSURANCE_LIMIT;
    public static final ModConfigSpec.IntValue INSURANCE_TERM_DAYS;
    public static final ModConfigSpec.DoubleValue INSURANCE_REACH;

    public static final ModConfigSpec.DoubleValue GREEN_SECONDS;
    public static final ModConfigSpec.DoubleValue YELLOW_SECONDS;
    public static final ModConfigSpec.DoubleValue ALL_RED_SECONDS;
    public static final ModConfigSpec.DoubleValue RED_LIGHT_FINE;
    public static final ModConfigSpec.DoubleValue SPEEDING_FINE;

    public static final ModConfigSpec.DoubleValue POSTAGE;
    public static final ModConfigSpec.DoubleValue MAIL_REACH;

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
                .comment("MJ per bucket for a fluid in the reikocars:gasoline or reikocars:diesel tag that BuildCraft has no fuel value for.")
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
        ROAD_SPEED = builder
                .comment("Top speed on asphalt (the reikocars:road block tag), as a multiple of full speed. Tires also wear a quarter",
                        "slower there. Set to 1.0 for no speed bonus.")
                .defineInRange("roadSpeedFactor", 1.15, 1.0, 2.0);
        RUN_OVER_DAMAGE = builder
                .comment("Damage a car does to a mob it hits, per block per tick of speed (a sedan at full speed goes about 0.9).",
                        "Players are only hurt when PvP is on. 0 turns it off.")
                .defineInRange("runOverDamage", 6.0, 0.0, 100.0);
        LOCK_NEW_CARS = builder
                .comment("Whether a newly placed car starts locked, so only its owner can get in until they unlock it.")
                .define("lockNewCars", true);
        REQUIRE_KEYS = builder
                .comment("Whether a car needs one of its keys in the driver's inventory to start, even for its owner (who cuts keys",
                        "from Blank Keys). If false, keys only let other players into locked cars and lock them.")
                .define("requireKeys", true);
        ALARM_SECONDS = builder
                .comment("How long a car alarm sounds, in seconds, once someone without a key hits a locked car or tries to get in.",
                        "0 turns car alarms off.")
                .defineInRange("alarmSeconds", 30, 0, 600);
        REAL_HEADLIGHTS = builder
                .comment("Whether headlights light up the road ahead in the dark (and police and ambulance beacons light up",
                        "the car while the siren's on), with invisible light blocks that move along with the car. If false,",
                        "the lights only glow.")
                .define("realHeadlights", true);
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

        builder.push("wear");
        WEAR = builder
                .comment("Multiplies wear and tear from driving: oil, engine, tires and brakes, cars' and trailers'. At 1, a car needs",
                        "an oil change about every 50,000 blocks, new tires every 80,000 and brakes after about 550 hard stops. 0 turns wear off.")
                .defineInRange("wearMultiplier", 1.0, 0.0, 100.0);
        CRASH_DAMAGE = builder
                .comment("Multiplies the body damage cars take hitting walls, other vehicles and mobs (and trailers in crashes and",
                        "jackknifes). At 1, a sedan driven into a wall at full speed loses 45% of its body. 0 turns crash damage off.")
                .defineInRange("crashDamageMultiplier", 1.0, 0.0, 10.0);
        BREAKDOWNS = builder
                .comment("Whether a car with a worn-out part (at 0%) breaks down and won't run until it's repaired (and a trailer can't be towed).",
                        "If false, worn parts only make it slower, thirstier and worse at stopping.")
                .define("breakdowns", true);
        BENCH_REACH = builder
                .comment("How close, in blocks, a car must be to a repair bench to be worked on.")
                .defineInRange("repairBenchReach", 6.0, 1.0, 32.0);
        DEFAULT_PART_PRICE = builder
                .comment("Price per part a newly placed repair bench charges for each kind of part. The owner can change them.")
                .defineInRange("defaultPricePerPart", 20.0, 0.0, 1_000_000.0);
        builder.pop();

        builder.push("fares");
        BUS_FARE = builder
                .comment("The fare a new city bus charges each passenger who gets on (plus sales tax), paid to the driver. Its owner can change it.")
                .defineInRange("busFare", 2.0, 0.0, 1_000_000.0);
        TAXI_START = builder
                .comment("What a new taxi's meter starts at for a ride. Its owner can change it.")
                .defineInRange("taxiStartingFare", 3.0, 0.0, 1_000_000.0);
        TAXI_PER_100 = builder
                .comment("What a new taxi's meter adds for every 100 blocks driven. Its owner can change it.")
                .defineInRange("taxiFarePer100Blocks", 1.0, 0.0, 1_000_000.0);
        builder.pop();

        builder.push("sales");
        DEALERSHIP_REACH = builder
                .comment("How close, in blocks, a car must be to a dealership to be on its lot: for sale there, or to trade in.")
                .defineInRange("dealershipReach", 10.0, 2.0, 48.0);
        builder.pop();

        builder.push("parking");
        PARKING_RATE = builder
                .comment("What a new parking meter charges per 10 minutes. Its owner can change it.")
                .defineInRange("ratePer10Minutes", 0.5, 0.0, 1_000_000.0);
        PARKING_FINE = builder
                .comment("A new parking meter's fine for overstaying, taken from the car owner's bank account. Its owner can change it.")
                .defineInRange("fine", 10.0, 0.0, 1_000_000.0);
        PARKING_GRACE = builder
                .comment("Seconds a car can stand at a meter, unpaid or after its time runs out, before it's ticketed.")
                .defineInRange("graceSeconds", 60, 0, 3600);
        PARKING_FINE_EVERY = builder
                .comment("Seconds between further tickets for a car that stays at a meter unpaid.")
                .defineInRange("fineEverySeconds", 900, 30, 86_400);
        builder.pop();

        builder.push("tolls");
        TOLL = builder
                .comment("The toll a new toll booth charges to lift its gates. Its owner can change it.")
                .defineInRange("toll", 2.0, 0.0, 1_000_000.0);
        builder.pop();

        builder.push("insurance");
        INSURANCE_PREMIUM = builder
                .comment("What a new insurance office charges for one term of cover on a car. Its owner can change it.")
                .defineInRange("premium", 10.0, 0.0, 1_000_000.0);
        INSURANCE_DEDUCTIBLE = builder
                .comment("A new insurance office's deductible: the part of each claim the car's owner pays themselves.")
                .defineInRange("deductible", 10.0, 0.0, 1_000_000.0);
        INSURANCE_LIMIT = builder
                .comment("The most a new insurance office pays out on one claim.")
                .defineInRange("claimLimit", 100.0, 0.0, 1_000_000.0);
        INSURANCE_TERM_DAYS = builder
                .comment("How many Minecraft days one term of cover lasts.")
                .defineInRange("termDays", 7, 1, 365);
        INSURANCE_REACH = builder
                .comment("How close, in blocks, a car must be parked to an insurance office to be insured there.")
                .defineInRange("officeReach", 10.0, 2.0, 64.0);
        builder.pop();

        builder.push("roads");
        GREEN_SECONDS = builder
                .comment("How long a traffic light stays green, in seconds. All lights share one clock: those facing north or south",
                        "are green while those facing east or west are red, and the other way round.")
                .defineInRange("greenSeconds", 10.0, 1.0, 300.0);
        YELLOW_SECONDS = builder
                .comment("How long a traffic light shows yellow before it turns red, in seconds.")
                .defineInRange("yellowSeconds", 3.0, 0.5, 30.0);
        ALL_RED_SECONDS = builder
                .comment("How long every light at a crossing shows red between one way's yellow and the other's green, in seconds.")
                .defineInRange("allRedSeconds", 1.0, 0.0, 30.0);
        RED_LIGHT_FINE = builder
                .comment("A new traffic light's red-light camera fine, paid from the car owner's bank account to the light's owner.",
                        "Its owner can change it, or set 0 to turn the camera off.")
                .defineInRange("redLightFine", 15.0, 0.0, 1_000_000.0);
        SPEEDING_FINE = builder
                .comment("A new speed limit sign's speed camera fine. Its owner can change it, or set 0 to turn the camera off.")
                .defineInRange("speedingFine", 20.0, 0.0, 1_000_000.0);
        builder.pop();

        builder.push("mail");
        POSTAGE = builder
                .comment("Postage for a parcel sent by mail van, paid by the sender (plus sales tax) and earned by whoever delivers it.",
                        "Dropping a parcel into its addressee's own mailbox by hand is always free.")
                .defineInRange("postage", 2.0, 0.0, 1_000_000.0);
        MAIL_REACH = builder
                .comment("How close, in blocks, the mail van must be to a mailbox to collect and deliver its mail.")
                .defineInRange("mailVanReach", 3.0, 1.0, 16.0);
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
