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

    public static final ModConfigSpec.DoubleValue DEFAULT_CHARGE_PRICE;
    public static final ModConfigSpec.IntValue CHARGER_CAPACITY;
    public static final ModConfigSpec.IntValue WATER_TANK;
    public static final ModConfigSpec.DoubleValue HOSE_REACH;
    public static final ModConfigSpec.IntValue HOSE_FLOW;
    public static final ModConfigSpec.IntValue LADDER_HEIGHT;
    public static final ModConfigSpec.IntValue HYDRANT_CAPACITY;
    public static final ModConfigSpec.BooleanValue ENDLESS_HYDRANTS;
    public static final ModConfigSpec.IntValue PAID_FIRES_PER_DAY;
    public static final ModConfigSpec.BooleanValue LOANS_ENABLED;
    public static final ModConfigSpec.DoubleValue LOAN_DOWN_PAYMENT;
    public static final ModConfigSpec.DoubleValue LOAN_INTEREST;
    public static final ModConfigSpec.IntValue LOAN_DAYS;
    public static final ModConfigSpec.IntValue LOAN_MISSES;
    public static final ModConfigSpec.IntValue LOAN_RECLAIM_DAYS;
    public static final ModConfigSpec.DoubleValue IMPOUND_LOT_RADIUS;
    public static final ModConfigSpec.DoubleValue IMPOUND_FEE;
    public static final ModConfigSpec.DoubleValue RENTAL_LOT_RADIUS;
    public static final ModConfigSpec.DoubleValue RENTAL_PRICE;
    public static final ModConfigSpec.IntValue RENTAL_MAX_DAYS;
    public static final ModConfigSpec.BooleanValue LICENSE_REQUIRED;
    public static final ModConfigSpec.DoubleValue UNLICENSED_FINE;
    public static final ModConfigSpec.IntValue RED_LIGHT_POINTS;
    public static final ModConfigSpec.IntValue SPEEDING_POINTS;
    public static final ModConfigSpec.IntValue MAX_POINTS;
    public static final ModConfigSpec.IntValue POINTS_DAYS;
    public static final ModConfigSpec.IntValue SUSPENSION_DAYS;
    public static final ModConfigSpec.IntValue TEST_MINUTES;
    public static final ModConfigSpec.DoubleValue ROUTE_RADIUS;
    public static final ModConfigSpec.DoubleValue TEST_FEE;
    public static final ModConfigSpec.DoubleValue REPLACE_FEE;
    public static final ModConfigSpec.DoubleValue DMV_REACH;
    public static final ModConfigSpec.DoubleValue CUSTOM_PLATE_PRICE;
    public static final ModConfigSpec.DoubleValue TUNING_REACH;
    public static final ModConfigSpec.DoubleValue DEFAULT_KIT_PRICE;
    public static final ModConfigSpec.DoubleValue TRACK_RADIUS;
    public static final ModConfigSpec.DoubleValue ENTRY_FEE;
    public static final ModConfigSpec.DoubleValue HOUSE_CUT;
    public static final ModConfigSpec.IntValue RACE_LAPS;
    public static final ModConfigSpec.IntValue RACE_MINUTES;

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

        builder.push("charging");
        DEFAULT_CHARGE_PRICE = builder
                .comment("Price per 1,000 MJ a newly placed charging station starts at. The owner can change it. An electric car's",
                        "battery holds 30,000 to 200,000 MJ.")
                .defineInRange("defaultPricePer1000MJ", 0.05, 0.0, 1_000_000.0);
        CHARGER_CAPACITY = builder
                .comment("MJ a charging station stores, from BuildCraft engines and kinesis pipes (or other mods' energy cables). It",
                        "charges cars from that store, so a few engines can keep it topped up between customers. Cars must be",
                        "within pumps.hoseLength blocks of it to charge.")
                .defineInRange("capacityMJ", 500_000, 1_000, 100_000_000);
        builder.pop();

        builder.push("fireTruck");
        WATER_TANK = builder
                .comment("Buckets of water a fire truck's tank holds.")
                .defineInRange("waterTankBuckets", 10, 1, 100);
        HOSE_REACH = builder
                .comment("How far, in blocks, someone with a fire hose can be from their fire truck and still spray.")
                .defineInRange("hoseLength", 16.0, 2.0, 64.0);
        HOSE_FLOW = builder
                .comment("Millibuckets of water the fire hose sprays a tick (20 ticks a second). At 10, a full tank of 10 buckets",
                        "lasts 50 seconds.")
                .defineInRange("hoseFlow", 10, 1, 1000);
        LADDER_HEIGHT = builder
                .comment("How many blocks up from the ground the fire truck's ladder reaches, fully out. It stops short of anything",
                        "above it.")
                .defineInRange("ladderHeight", 16, 7, 17);
        HYDRANT_CAPACITY = builder
                .comment("Buckets of water a fire hydrant holds, piped in by BuildCraft (or any mod's) pipes from a pump.")
                .defineInRange("hydrantCapacityBuckets", 8, 1, 1000);
        ENDLESS_HYDRANTS = builder
                .comment("Whether fire hydrants never run dry, as if on the water mains, with no pipes needed.")
                .define("endlessHydrants", false);
        PAID_FIRES_PER_DAY = builder
                .comment("How many fires a day (a Minecraft day) the Firefighter job pays each player for putting out. Fires near",
                        "where they lit one themselves in the last ten minutes, and fires that never go out (on netherrack and",
                        "the like), never pay.")
                .defineInRange("paidFiresPerDay", 64, 0, 100_000);
        builder.pop();

        builder.push("loans");
        LOANS_ENABLED = builder
                .comment("Whether cars for sale on a dealership's lot can be bought on finance: a down payment now, the rest a day at a time.")
                .define("enabled", true);
        LOAN_DOWN_PAYMENT = builder
                .comment("The share of the price paid down (0.2 is 20%). Sales tax on the whole price is paid with it.")
                .defineInRange("downPayment", 0.2, 0.0, 1.0);
        LOAN_INTEREST = builder
                .comment("Interest on the rest, all told (0.1 is 10% on top).")
                .defineInRange("interestRate", 0.1, 0.0, 10.0);
        LOAN_DAYS = builder
                .comment("How many daily payments (one a Minecraft day, from the bank account to the dealer) it's paid off in.")
                .defineInRange("days", 10, 1, 1000);
        LOAN_MISSES = builder
                .comment("Missed payments in a row before the car is repossessed: taken to the impound lot nearest the dealership.")
                .defineInRange("missedPaymentsToRepossess", 3, 1, 100);
        LOAN_RECLAIM_DAYS = builder
                .comment("Days a repossessed car's owner has to get it back (paying the missed payments and the lot's fee) before",
                        "it's the dealer's.")
                .defineInRange("reclaimDays", 5, 1, 1000);
        builder.pop();

        builder.push("impound");
        IMPOUND_LOT_RADIUS = builder
                .comment("How far from an impound office its lot reaches, in blocks: a towed car put down within it is impounded.")
                .defineInRange("lotRadius", 10.0, 2.0, 64.0);
        IMPOUND_FEE = builder
                .comment("The release fee a newly placed impound office starts at. Its owner can change it.")
                .defineInRange("defaultReleaseFee", 50.0, 0.0, 1_000_000.0);
        builder.pop();

        builder.push("rentals");
        RENTAL_LOT_RADIUS = builder
                .comment("How far from a rental desk its lot reaches, in blocks: the owner's cars parked within it are its fleet.")
                .defineInRange("lotRadius", 10.0, 2.0, 64.0);
        RENTAL_PRICE = builder
                .comment("A day's rent for every kind of car at a newly placed rental desk. Its owner can change each.")
                .defineInRange("defaultPricePerDay", 15.0, 0.0, 1_000_000.0);
        RENTAL_MAX_DAYS = builder
                .comment("The most Minecraft days (20 minutes each) a car can be rented for at once.")
                .defineInRange("maxDays", 7, 1, 100);
        builder.pop();

        builder.push("licenses");
        LICENSE_REQUIRED = builder
                .comment("Whether a player needs a valid driver's license (passed at a DMV, not suspended) to drive at all. If false,",
                        "anyone can drive, but cameras and police fine unlicensed drivers (see unlicensedFine).")
                .define("required", false);
        UNLICENSED_FINE = builder
                .comment("The fine for driving without a valid license: added when a red-light or speed camera catches an unlicensed",
                        "driver (paid to the camera's owner), and what a police officer's Ticket Book charges. 0 turns both off.")
                .defineInRange("unlicensedFine", 25.0, 0.0, 1_000_000.0);
        RED_LIGHT_POINTS = builder
                .comment("Penalty points on the driver's license for running a red light caught on camera.")
                .defineInRange("redLightPoints", 3, 0, 100);
        SPEEDING_POINTS = builder
                .comment("Penalty points on the driver's license for speeding caught on camera.")
                .defineInRange("speedingPoints", 2, 0, 100);
        MAX_POINTS = builder
                .comment("Points that suspend a license.")
                .defineInRange("maxPoints", 12, 1, 1000);
        POINTS_DAYS = builder
                .comment("How many Minecraft days (20 minutes each) penalty points stay on a license.")
                .defineInRange("pointsLastDays", 7, 1, 1000);
        SUSPENSION_DAYS = builder
                .comment("How many Minecraft days a suspension lasts.")
                .defineInRange("suspensionDays", 3, 1, 1000);
        TEST_MINUTES = builder
                .comment("Minutes (real time) a driving test allows to get round the DMV's cones.")
                .defineInRange("testMinutes", 5, 1, 60);
        ROUTE_RADIUS = builder
                .comment("How far from a DMV its owner's numbered traffic cones can be and still be on its test route.")
                .defineInRange("routeRadius", 128.0, 8.0, 1024.0);
        TEST_FEE = builder
                .comment("What a newly placed DMV charges for the driving test (the license comes with passing). Its owner can change it.")
                .defineInRange("defaultTestFee", 25.0, 0.0, 1_000_000.0);
        REPLACE_FEE = builder
                .comment("What a newly placed DMV charges for a replacement license card. Its owner can change it.")
                .defineInRange("defaultReplaceFee", 10.0, 0.0, 1_000_000.0);
        DMV_REACH = builder
                .comment("How close to a DMV a car must be parked for its owner to buy it a custom plate there.")
                .defineInRange("dmvReach", 10.0, 2.0, 64.0);
        builder.pop();

        builder.push("tuning");
        TUNING_REACH = builder
                .comment("How close a car must be to a Tuning Bench to be worked on.")
                .defineInRange("benchReach", 6.0, 2.0, 32.0);
        DEFAULT_KIT_PRICE = builder
                .comment("What a newly placed Tuning Bench charges to fit each kind of kit from its stock. Its owner can change each.")
                .defineInRange("defaultKitPrice", 40.0, 0.0, 1_000_000.0);
        builder.pop();

        builder.push("racing");
        TRACK_RADIUS = builder
                .comment("How far from a Race Control its owner's numbered race flags can be and still be on its track.")
                .defineInRange("trackRadius", 256.0, 16.0, 2048.0);
        ENTRY_FEE = builder
                .comment("A newly placed Race Control's entry fee for a race (it all goes in the pot). Its owner can change it.")
                .defineInRange("defaultEntryFee", 10.0, 0.0, 1_000_000.0);
        HOUSE_CUT = builder
                .comment("The share of a race's pot a newly placed Race Control's owner keeps (0.1 is 10%); the winner gets the rest.")
                .defineInRange("defaultHouseCut", 0.1, 0.0, 1.0);
        RACE_LAPS = builder
                .comment("Laps in a race at a newly placed Race Control. Its owner can change it (1 to 10).")
                .defineInRange("defaultLaps", 3, 1, 10);
        RACE_MINUTES = builder
                .comment("How long a race or time trial can run, in minutes, before whoever hasn't finished is out.")
                .defineInRange("raceMinutes", 10, 1, 60);
        builder.pop();

        builder.push("plates");
        CUSTOM_PLATE_PRICE = builder
                .comment("What a newly placed DMV charges for a custom license plate. Its owner can change it.")
                .defineInRange("defaultCustomPlatePrice", 100.0, 0.0, 1_000_000.0);
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
