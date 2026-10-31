package com.reiko.cars.car;

import static com.reiko.cars.car.FuelType.DIESEL;
import static com.reiko.cars.car.FuelType.ELECTRIC;
import static com.reiko.cars.car.FuelType.GASOLINE;

import java.util.List;
import java.util.Locale;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * The kinds of car and how each one drives. Speeds are in blocks per tick (1.0 is 72 km/h), fuel in BuildCraft MJ.
 *
 * <p>Seats are where each passenger's hips go, in model pixels (16 to a block) measured from the middle of the car
 * on the ground: {@code x} toward the car's left (the driver's side), {@code y} up, {@code z} toward the front. They
 * match the seats in the Blockbench models in {@code blockbench/}; move both together.
 *
 * <p>Tank sizes are in buckets of the car's own fuel: light fuel for gasoline cars, dense fuel for diesel ones; an
 * electric car's battery is in 10,000s of MJ. A
 * tractor's fifth wheel, where a trailer's kingpin sits, is given in pixels behind the middle and up from the ground.
 */
public enum CarType {
    //          width  height length wheel  top    accel   brake   reverse turn  tank MJ/block colour  seat off-road bed wheels fuel   fifth wheel
    SEDAN(       2.25f, 1.8f,  3.5f,  5,    0.90f, 0.012f, 0.040f, 0.25f, 4.0f,  8, 144, 0x2E5EAA,  7, false, 0, 4, GASOLINE, noFifthWheel(),
            seat(6, -2), seat(-6, -2), seat(6, 9), seat(-6, 9)),
    TAXI(        2.25f, 2.2f,  3.5f,  5,    0.90f, 0.012f, 0.040f, 0.25f, 4.0f,  8, 144, 0xF5C518,  7, false, 0, 4, GASOLINE, noFifthWheel(),
            seat(6, -2), seat(-6, -2), seat(6, 9), seat(-6, 9)),
    SPORTS_CAR(  2.25f, 1.6f,  3.5f,  5,    1.30f, 0.020f, 0.050f, 0.25f, 4.5f,  6, 216, 0xC0262D,  5, false, 0, 4, GASOLINE, noFifthWheel(),
            seat(6, 4), seat(-6, 4)),
    PICKUP_TRUCK(2.4f,  2.0f,  3.75f, 6,    0.80f, 0.010f, 0.035f, 0.22f, 3.5f, 12, 180, 0xE9E9E4, 10, false, 3, 4, GASOLINE, noFifthWheel(),
            seat(6, -1), seat(-6, -1)),
    OFF_ROADER(  2.4f,  2.05f, 3.6f,  7,    0.85f, 0.014f, 0.040f, 0.25f, 4.0f, 10, 168, 0xC9A15A, 11, true,  0, 4, GASOLINE, noFifthWheel(),
            seat(6, -3), seat(-6, -3), seat(6, 11), seat(-6, 11)),
    /** Small and cheap, and easy on fuel. */
    HATCHBACK(   2.1f,  1.75f, 3.1f,  5,    0.80f, 0.013f, 0.040f, 0.25f, 4.6f,  6, 110, 0x7CB342,  7, false, 0, 4, GASOLINE, noFifthWheel(),
            seat(6, -1), seat(-6, -1), seat(6, 11), seat(-6, 11)),
    /** A big V8: fast in a straight line, slow to turn, thirsty. */
    MUSCLE_CAR(  2.3f,  1.5f,  3.9f,  6,    1.20f, 0.024f, 0.040f, 0.25f, 3.6f,  8, 250, 0xF57C00,  6, false, 0, 4, GASOLINE, noFifthWheel(),
            seat(6, 2), seat(-6, 2)),
    /** An open-top roadster. */
    CONVERTIBLE( 2.2f,  1.4f,  3.6f,  5,    1.15f, 0.019f, 0.048f, 0.25f, 4.8f,  6, 170, 0x26A69A,  6, false, 0, 4, GASOLINE, noFifthWheel(),
            seat(6, 3), seat(-6, 3)),
    /** Three rows of seats and a trunk. */
    MINIVAN(     2.35f, 2.1f,  4.1f,  6,    0.75f, 0.010f, 0.035f, 0.22f, 3.6f, 10, 160, 0xB0BEC5, 10, false, 1, 4, GASOLINE, noFifthWheel(),
            seat(6, -12), seat(-6, -12), seat(6, 1), seat(-6, 1), seat(6, 15), seat(-6, 15)),
    /** A semi tractor: pulls trailers on the fifth wheel behind its sleeper. Its tank is in buckets of dense fuel. */
    SEMI_TRUCK(  2.4f,  3.6f,  6.25f, 8,    0.85f, 0.010f, 0.035f, 0.20f, 3.2f, 12, 1440, 0x2E6B3A, 25, false, 0, 4, DIESEL, fifthWheel(32, 18),
            seat(9, -12), seat(-9, -12)),
    /** A tall van with a 54-slot cargo area behind the cab. */
    DELIVERY_VAN(2.4f,  2.6f,  4.45f, 6,    0.75f, 0.010f, 0.035f, 0.22f, 3.4f, 10, 900, 0xF5F5F5, 16, false, 6, 4, DIESEL, noFifthWheel(),
            seat(7, -15), seat(-7, -15)),
    /** Ten seats: the driver's, four rows of two, and the back bench. Turns about its back axle. */
    CITY_BUS(    2.6f,  3.4f,  7.75f, 7,    0.65f, 0.008f, 0.030f, 0.18f, 2.8f, 16, 1200, 0xFFB300, 18, false, 0, 4, DIESEL, noFifthWheel(),
            seat(10, -49), seat(8, -30), seat(-8, -30), seat(8, -14), seat(-8, -14), seat(8, 2), seat(-8, 2), seat(8, 18), seat(-8, 18),
            seat(0, 44)),
    /** A lifted crew-cab 4x4 with a truck bed. */
    HEAVY_PICKUP(2.5f,  2.45f, 4.5f,  8,    0.85f, 0.013f, 0.040f, 0.22f, 3.8f, 14, 600, 0xB71C1C, 15, true,  3, 4, DIESEL, noFifthWheel(),
            seat(7, -8), seat(-7, -8), seat(7, 4), seat(-7, 4)),
    /** Slow, but at home in a field. */
    FARM_TRACTOR(2.2f,  2.95f, 3.5f,  8,    0.45f, 0.010f, 0.040f, 0.15f, 4.5f,  6, 500, 0x388E3C, 24, true,  0, 4, DIESEL, noFifthWheel(),
            seat(0, 9)),
    /** A rollback tow truck: carries a car on its long flat deck. */
    FLATBED(     2.5f,  3.15f, 7.35f, 7,    0.80f, 0.010f, 0.035f, 0.20f, 3.0f, 14, 950, 0xC62828, 26, false, 1, 4, DIESEL, noFifthWheel(),
            seat(8, -17), seat(-8, -17)),
    /** A tow truck with a boom and a wheel-lift: lifts a car by its front wheels and tows it behind. */
    WRECKER(     2.5f,  3.4f,  7.6f,  7,    0.85f, 0.011f, 0.036f, 0.20f, 3.1f, 12, 900, 0xF2A900, 26, false, 2, 4, DIESEL, noFifthWheel(),
            seat(8, -17), seat(-8, -17)),
    /** A tall walk-in mail van, driven from the right-hand seat, with a mail compartment in the back. */
    MAIL_VAN(    2.35f, 3.05f, 4.75f, 6,    0.78f, 0.012f, 0.038f, 0.22f, 3.9f,  9, 150, 0xF4F5F7, 15, false, 4, 4, GASOLINE, noFifthWheel(),
            seat(-7, -16), seat(7, -16)),
    /** The sedan as a police car: quicker, with a light bar and siren (K). */
    POLICE_CAR(  2.25f, 2.0f,  3.75f, 5,    1.05f, 0.016f, 0.046f, 0.25f, 4.1f,  9, 160, 0x16181C,  7, false, 1, 4, GASOLINE, noFifthWheel(),
            seat(6, -2), seat(-6, -2), seat(6, 9), seat(-6, 9)),
    /** A box ambulance: lights and siren (K), and it slowly heals whoever rides in the back. */
    AMBULANCE(   2.4f,  2.75f, 4.6f,  6,    0.85f, 0.012f, 0.038f, 0.22f, 3.5f, 10, 900, 0xF4F5F7, 16, false, 1, 4, DIESEL, noFifthWheel(),
            seat(7, -15), seat(-7, -15), seat(6, 14), seat(-9, 6), seat(-9, 18)),
    /** A rear-loading garbage truck: empties trash cans with G. */
    GARBAGE_TRUCK(2.5f, 3.15f, 7.8f,  7,    0.70f, 0.009f, 0.033f, 0.18f, 2.9f, 14, 1000, 0x388E3C, 26, false, 0, 4, DIESEL, noFifthWheel(),
            seat(8, -17), seat(-8, -17)),
    /** An ice cream van: sells what's in its freezer from the serving hatch, and the horn plays a jingle. */
    ICE_CREAM_TRUCK(2.35f, 3.1f, 4.75f, 6,  0.72f, 0.011f, 0.036f, 0.22f, 3.8f,  9, 150, 0xFFF3E0, 15, false, 3, 4, GASOLINE, noFifthWheel(),
            seat(-7, -16), seat(7, -16)),
    /** A dump truck: a 54-slot bed that tips its load out behind with G. */
    DUMP_TRUCK(  2.5f,  3.15f, 7.4f,  7,    0.72f, 0.009f, 0.034f, 0.18f, 2.9f, 16, 1100, 0xF57C00, 26, true,  6, 4, DIESEL, noFifthWheel(),
            seat(8, -17), seat(-8, -17)),
    /** A cement mixer: its drum turns concrete powder into concrete while the engine runs; G pours it out. */
    CEMENT_MIXER(2.5f,  3.4f,  7.9f,  7,    0.68f, 0.009f, 0.033f, 0.18f, 2.9f, 16, 1150, 0xF4F5F7, 26, false, 2, 4, DIESEL, noFifthWheel(),
            seat(8, -17), seat(-8, -17)),
    /**
     * A fire engine: lights and siren (K), a water tank for the fire hose, filled at a hydrant, from a lake or by the
     * bucket, and an aerial ladder that raises up the back (G). A crew cab: two up front, a bench of two behind.
     */
    FIRE_TRUCK(  2.5f,  3.4f,  7.6f,  7,    0.85f, 0.011f, 0.036f, 0.20f, 3.0f, 14, 1000, 0xC62828, 26, false, 2, 4, DIESEL, noFifthWheel(),
            seat(8, -40), seat(-8, -40), seat(7, -24), seat(-7, -24)),
    /**
     * A road paver: with its screed down (G) it lays a road behind it as it's driven, from what's in its hopper (see
     * {@code com.reiko.cars.road.RoadPaver}). One seat, up on its deck.
     */
    ROAD_PAVER(  2.6f,  3.25f, 4.9f,  9,    0.40f, 0.008f, 0.040f, 0.15f, 3.6f, 10, 800, 0xF2C014, 33, true,  3, 4, DIESEL, noFifthWheel(),
            seat(0, 14)),

    // Electric: charged at a charging station. Quick off the line, quiet, and they get a little back when braking.
    /** A tall little two-seater for town: nippy, turns on a coin, and sips its battery. */
    CITY_EV(     2.0f,  1.75f, 2.8f,  5,    0.75f, 0.018f, 0.042f, 0.25f, 5.2f,  8,  32, 0x4FC3F7,  7, false, 1, 4, ELECTRIC, noFifthWheel(),
            seat(6, 1), seat(-6, 1)),
    /** A fast, smooth electric fastback: the quickest car off the line, a glass roof and a frunk. */
    ELECTRIC_SEDAN(2.25f, 1.65f, 3.8f, 5,   1.25f, 0.030f, 0.050f, 0.25f, 4.3f, 16,  45, 0xEDEEF0,  7, false, 1, 4, ELECTRIC, noFifthWheel(),
            seat(6, -2), seat(-6, -2), seat(6, 10), seat(-6, 10)),
    /** A crew-cab electric pickup with a motor on each axle: at home off-road, with a truck bed. */
    ELECTRIC_PICKUP(2.5f, 2.2f, 4.3f,  7,   0.95f, 0.021f, 0.042f, 0.22f, 3.7f, 20,  70, 0x3C6E57, 12, true,  3, 4, ELECTRIC, noFifthWheel(),
            seat(7, -6), seat(-7, -6), seat(7, 4), seat(-7, 4)),
    /** An electric delivery van with a 54-slot cargo area. */
    ELECTRIC_VAN(2.4f,  2.7f,  4.6f,  6,    0.80f, 0.016f, 0.036f, 0.22f, 3.6f, 16,  60, 0x5FB4E5, 16, false, 6, 4, ELECTRIC, noFifthWheel(),
            seat(7, -17), seat(-7, -17)),

    // Motorcycles: two wheels, lean into turns, a rider and maybe a passenger behind.
    /** The fastest thing on the road, and quick off the line. */
    SPORT_BIKE(  1.1f,  1.4f,  2.25f, 6,    1.45f, 0.026f, 0.055f, 0.10f, 5.5f,  4, 110, 0x1E88E5, 15, false, 0, 2, GASOLINE, noFifthWheel(),
            seat(0, 3), seat(0, 10)),
    /** Long, low and loud, with saddlebags. */
    CRUISER(     1.1f,  1.5f,  2.6f,  6,    1.00f, 0.016f, 0.040f, 0.10f, 4.5f,  5, 100, 0x262626, 12, false, 1, 2, GASOLINE, noFifthWheel(),
            seat(0, 2), seat(0, 9)),
    /** Keeps its speed off-road. */
    DIRT_BIKE(   1.1f,  1.4f,  2.5f,  7,    0.95f, 0.022f, 0.050f, 0.10f, 6.0f,  3,  90, 0xF2C014, 18, true,  0, 2, GASOLINE, noFifthWheel(),
            seat(0, 3)),
    /** Slow, but goes a long way on a little fuel. */
    SCOOTER(     1.0f,  1.3f,  1.8f,  4,    0.60f, 0.014f, 0.040f, 0.08f, 6.5f,  2,  36, 0x7FD1C6, 13, false, 0, 2, GASOLINE, noFifthWheel(),
            seat(0, 6)),
    /** A step-through electric moped with room for two: quiet, and goes a long way on a charge. */
    ELECTRIC_MOPED(1.0f, 1.3f, 2.0f,  4,    0.70f, 0.020f, 0.045f, 0.08f, 6.5f,  3,  12, 0xF2F2F2, 13, false, 0, 2, ELECTRIC, noFifthWheel(),
            seat(0, 5), seat(0, 12));

    /**
     * How far a seated player's model hangs below the attachment point, in pixels: a player sits 0.6 blocks lower
     * than where they're attached, and their (15/16-scale) legs put the hips 11.25 pixels above their feet.
     */
    private static final double HIPS_ABOVE_ATTACHMENT = 11.25 - 9.6;

    private final String id;
    public final float width;
    public final float height;
    /** Bumper to bumper, in blocks. The hitbox is square, so the nose and tail stick out past it a little. */
    public final float length;
    /** Wheel radius in model pixels, for spinning the wheels at the right rate. */
    private final int wheelRadius;
    public final float topSpeed;
    public final float acceleration;
    public final float braking;
    public final float reverseSpeed;
    /** Degrees a tick at full lock and walking pace or faster. */
    public final float turnRate;
    public final int tankBuckets;
    public final double fuelPerBlock;
    public final int defaultColor;
    /** Height of the seat cushions' top, where passengers' hips rest, in model pixels. */
    private final int seatHeight;
    /** Keeps its speed on grass, sand and snow. */
    public final boolean offRoad;
    /** Rows of 9 slots in the truck bed (or saddlebags), or 0 for none. */
    public final int storageRows;
    /** 4 for a car, 2 for a motorcycle. */
    public final int wheels;
    private final FuelType fuel;
    @Nullable
    private final int[] fifthWheel;
    private final List<int[]> seats;

    CarType(float width, float height, float length, int wheelRadius, float topSpeed, float acceleration, float braking, float reverseSpeed,
            float turnRate, int tankBuckets, double fuelPerBlock, int defaultColor, int seatHeight, boolean offRoad,
            int storageRows, int wheels, FuelType fuel, @Nullable int[] fifthWheel, int[]... seats) {
        this.id = name().toLowerCase(Locale.ROOT);
        this.width = width;
        this.height = height;
        this.length = length;
        this.wheelRadius = wheelRadius;
        this.topSpeed = topSpeed;
        this.acceleration = acceleration;
        this.braking = braking;
        this.reverseSpeed = reverseSpeed;
        this.turnRate = turnRate;
        this.tankBuckets = tankBuckets;
        this.fuelPerBlock = fuelPerBlock;
        this.defaultColor = defaultColor;
        this.seatHeight = seatHeight;
        this.offRoad = offRoad;
        this.storageRows = storageRows;
        this.wheels = wheels;
        this.fuel = fuel;
        this.fifthWheel = fifthWheel;
        this.seats = List.of(seats);
    }

    /** An ordinary car, which can't pull a trailer. */
    @Nullable
    private static int[] noFifthWheel() {
        return null;
    }

    private static int[] fifthWheel(int back, int height) {
        return new int[] {back, height};
    }

    /** A seat {@code x} pixels to the car's left and {@code back} pixels behind its middle. */
    private static int[] seat(int x, int back) {
        return new int[] {x, back};
    }

    /** The registry name, like {@code sedan}. */
    public String id() {
        return id;
    }

    /** Wheel radius in blocks. */
    public double wheelRadius() {
        return wheelRadius / 16.0;
    }

    public int seatCount() {
        return seats.size();
    }

    /** "1 seat" or "4 seats". */
    public net.minecraft.network.chat.Component seatsText() {
        return seats.size() == 1 ? net.minecraft.network.chat.Component.translatable("gui.reikocars.seats.one")
                : net.minecraft.network.chat.Component.translatable("gui.reikocars.seats", seats.size());
    }

    /** What it runs on: gasoline, diesel or electricity. */
    public FuelType fuel() {
        return fuel;
    }

    /** Runs on a battery, charged at a charging station. */
    public boolean isElectric() {
        return fuel.isElectric();
    }

    /** Fuel a full tank holds, in MJ. */
    public double tankCapacity() {
        return tankBuckets * fuel.bucketEnergy;
    }

    /** A motorcycle: two wheels, leans into turns. */
    /** Whether its riders sit in the open, so people nearby hear its radio better: the convertible, the tractor, the bikes. */
    public boolean openTop() {
        return this == CONVERTIBLE || this == FARM_TRACTOR || isBike();
    }

    public boolean isBike() {
        return wheels == 2;
    }

    /** What its storage is called: a pickup's truck bed, the cruiser's saddlebags, the minivan's trunk... */
    public String storageKey() {
        return switch (this) {
            case CRUISER -> "container.reikocars.saddlebags";
            case MINIVAN -> "container.reikocars.trunk";
            case DELIVERY_VAN -> "container.reikocars.cargo_area";
            case FLATBED, WRECKER -> "container.reikocars.toolboxes";
            case MAIL_VAN -> "container.reikocars.mail_compartment";
            case POLICE_CAR -> "container.reikocars.trunk";
            case AMBULANCE -> "container.reikocars.medical_cabinet";
            case ICE_CREAM_TRUCK -> "container.reikocars.freezer";
            case DUMP_TRUCK -> "container.reikocars.dump_bed";
            case CEMENT_MIXER -> "container.reikocars.drum";
            case FIRE_TRUCK -> "container.reikocars.compartments";
            case ROAD_PAVER -> "container.reikocars.hopper";
            case CITY_EV -> "container.reikocars.trunk";
            case ELECTRIC_SEDAN -> "container.reikocars.frunk";
            case ELECTRIC_VAN -> "container.reikocars.cargo_area";
            default -> "container.reikocars.truck_bed";
        };
    }

    /** A tow truck: picks up broken-down (or any) cars with G. */
    public boolean isTowTruck() {
        return this == FLATBED || this == WRECKER;
    }

    /**
     * Whether a tow truck of type {@code truck} can pick this up: cars, vans and pickups up to about 4.8 blocks long,
     * but not semis, buses or other tow trucks. The flatbed carries motorcycles too; the wrecker lifts a car by its
     * front wheels, so it needs four.
     */
    public boolean towableBy(CarType truck) {
        if (!truck.isTowTruck() || isTowTruck() || isTractor() || this == CITY_BUS || length > 4.8f) {
            return false;
        }
        return truck == FLATBED || !isBike();
    }

    /** Whether it takes fares from its passengers: the city bus (a flat fare) and the taxi (a meter). */
    public boolean takesFares() {
        return this == CITY_BUS || this == TAXI;
    }

    /** Whether it runs a bus route, stopping at bus stops (the city bus). */
    public boolean runsRoutes() {
        return this == CITY_BUS;
    }

    /** Whether its fare runs on a meter, by distance (the taxi), rather than a flat fare when boarding. */
    public boolean isMetered() {
        return this == TAXI;
    }

    /** The police car, the ambulance and the fire truck: a light bar and siren, switched with K. */
    public boolean hasSiren() {
        return this == POLICE_CAR || this == AMBULANCE || this == FIRE_TRUCK;
    }

    /** The road paver: lays a road as it's driven (see {@code com.reiko.cars.road.RoadPaver}). */
    public boolean isPaver() {
        return this == ROAD_PAVER;
    }

    /** The fire truck: a water tank, the fire hose and the ladder (see {@code com.reiko.cars.fire}). */
    public boolean isFireTruck() {
        return this == FIRE_TRUCK;
    }

    /** The mail van: collects and delivers parcels at mailboxes with G. */
    public boolean isMailVan() {
        return this == MAIL_VAN;
    }

    /** Whether it fits on a car hauler: anything up to a minivan's size, but no trucks, vans or buses. */
    public boolean fitsOnHauler() {
        return !isTractor() && length <= 4.1f && fuel != FuelType.DIESEL;
    }

    /**
     * Where its exhaust stacks' tops are, for the smoke, relative to the vehicle facing south (+z): x to its left, z
     * forward, in blocks. Empty for one with a tailpipe at the back.
     */
    public List<Vec3> exhaustStacks() {
        return switch (this) {
            case SEMI_TRUCK -> List.of(new Vec3(1.19, 4.3, -0.94), new Vec3(-1.19, 4.3, -0.94));
            case FARM_TRACTOR -> List.of(new Vec3(-0.375, 2.6, 0.81));
            case FLATBED, WRECKER, GARBAGE_TRUCK, DUMP_TRUCK, CEMENT_MIXER -> List.of(new Vec3(-1.03, 3.31, 0.375));
            case ROAD_PAVER -> List.of(new Vec3(0.59, 2.8, 0.66));
            default -> List.of();
        };
    }

    /** The horn's pitch: a big truck's is low, a scooter's beeps. */
    public float hornPitch() {
        return switch (this) {
            case SPORTS_CAR, CONVERTIBLE -> 1.15F;
            case PICKUP_TRUCK, DELIVERY_VAN -> 0.85F;
            case SEMI_TRUCK, CITY_BUS -> 0.6F;
            case HATCHBACK -> 1.25F;
            case MUSCLE_CAR -> 0.9F;
            case MINIVAN -> 1.05F;
            case HEAVY_PICKUP, FARM_TRACTOR -> 0.75F;
            case SPORT_BIKE, DIRT_BIKE -> 1.35F;
            case CRUISER -> 1.1F;
            case SCOOTER -> 1.6F;
            case FLATBED, WRECKER -> 0.7F;
            case MAIL_VAN -> 1.1F;
            case POLICE_CAR -> 1.05F;
            case AMBULANCE -> 0.95F;
            case GARBAGE_TRUCK, DUMP_TRUCK, CEMENT_MIXER -> 0.68F;
            case FIRE_TRUCK -> 0.55F; // an air horn
            case ROAD_PAVER -> 0.7F;
            case CITY_EV -> 1.3F;
            case ELECTRIC_SEDAN -> 1.1F;
            case ELECTRIC_PICKUP -> 0.85F;
            case ELECTRIC_VAN -> 0.95F;
            case ELECTRIC_MOPED -> 1.5F;
            default -> 1.0F;
        };
    }

    /** The engine sound's pitch at idle, against a car's 1.0 (an electric motor's whine, for the electric ones). */
    public float enginePitch() {
        return switch (this) {
            case SEMI_TRUCK, CITY_BUS -> 0.7F;
            case DELIVERY_VAN -> 0.85F;
            case HEAVY_PICKUP, MUSCLE_CAR -> 0.78F;
            case FARM_TRACTOR -> 0.62F;
            case HATCHBACK -> 1.15F;
            case CONVERTIBLE -> 1.05F;
            case SPORT_BIKE -> 1.3F;
            case CRUISER -> 0.8F;
            case DIRT_BIKE -> 1.4F;
            case SCOOTER -> 1.6F;
            case FLATBED, WRECKER -> 0.75F;
            case MAIL_VAN, ICE_CREAM_TRUCK -> 1.05F;
            case POLICE_CAR -> 0.92F;
            case AMBULANCE -> 0.85F;
            case GARBAGE_TRUCK, DUMP_TRUCK, CEMENT_MIXER, FIRE_TRUCK -> 0.72F;
            case ROAD_PAVER -> 0.66F;
            case CITY_EV -> 1.15F;
            case ELECTRIC_PICKUP -> 0.85F;
            case ELECTRIC_VAN -> 0.9F;
            case ELECTRIC_MOPED -> 1.4F;
            default -> 1.0F;
        };
    }

    /** Whether it has a fifth wheel, to pull a semi trailer. */
    public boolean isTractor() {
        return fifthWheel != null;
    }

    /**
     * How far behind its middle the tow ball is, in pixels, for the pickups, SUVs (and the farm tractor) that pull the
     * bumper-pull trailers; 0 for a vehicle without one. Every ball is {@link com.reiko.cars.trailer.TrailerType#BALL_HEIGHT}
     * up, on a hitch out behind the bumper.
     */
    private float towBallBack() {
        return switch (this) {
            case PICKUP_TRUCK -> 35.5F;
            case OFF_ROADER -> 33.5F;
            case MINIVAN -> 36.5F;
            case HEAVY_PICKUP, ELECTRIC_PICKUP -> 39.5F;
            case FARM_TRACTOR -> 27.0F;
            default -> 0.0F;
        };
    }

    /** Whether it has a tow ball, for the camper and livestock trailer. */
    public boolean hasTowBall() {
        return towBallBack() > 0.0F;
    }

    /** Whether it can pull a trailer of {@code trailer}'s kind: a semi trailer on a fifth wheel, a bumper-pull on a ball. */
    public boolean canTow(com.reiko.cars.trailer.TrailerType trailer) {
        return trailer.isSemiTrailer() ? isTractor() : hasTowBall();
    }

    /** Whether it can pull any trailer at all. */
    public boolean canTowTrailers() {
        return isTractor() || hasTowBall();
    }

    /**
     * Where a trailer hitches, relative to the car's position facing south (+z), in blocks: the top of the fifth wheel,
     * or the tow ball. Rotate it by the car's yaw before use.
     */
    public Vec3 hitchOffset() {
        if (fifthWheel != null) {
            return fifthWheelOffset();
        }
        return new Vec3(0.0, com.reiko.cars.trailer.TrailerType.BALL_HEIGHT / 16.0, -towBallBack() / 16.0);
    }

    /**
     * Where a trailer's kingpin sits, relative to the car's position (the middle of its footprint, on the ground) when
     * it faces south (+z), in blocks. Rotate it by the car's yaw before use. Only for tractors.
     */
    /**
     * How far behind its middle the vehicle turns about, in blocks. A tractor turns about its back axles, which are
     * under the fifth wheel, as a real one does: then the fifth wheel follows the cab round a bend instead of being
     * flung out sideways, and the trailer follows it. The bus turns about its back axle too. Cars turn about their
     * middle.
     */
    public double pivotBack() {
        if (this == CITY_BUS) {
            return 36 / 16.0; // its back axle: the bus swings its long nose round corners, as a real one does
        }
        if (this == FLATBED) {
            return 26 / 16.0; // the tow trucks turn about their back axles too
        }
        if (this == WRECKER) {
            return 20 / 16.0;
        }
        if (this == GARBAGE_TRUCK) {
            return 30 / 16.0;
        }
        if (this == DUMP_TRUCK) {
            return 32 / 16.0; // between its two back axles
        }
        if (this == CEMENT_MIXER) {
            return 34 / 16.0;
        }
        if (this == FIRE_TRUCK) {
            return 33 / 16.0; // between its two back axles
        }
        if (this == ROAD_PAVER) {
            return 10 / 16.0; // its big drive wheels
        }
        return fifthWheel == null ? 0.0 : fifthWheel[0] / 16.0;
    }

    public Vec3 fifthWheelOffset() {
        return fifthWheel == null ? Vec3.ZERO : new Vec3(0.0, fifthWheel[1] / 16.0, -fifthWheel[0] / 16.0);
    }

    /**
     * Where seat {@code index}'s passenger is attached, in blocks relative to the car facing south (+z): x to the
     * car's left, z forward. Rotate it by the car's yaw before use.
     */
    public Vec3 seatOffset(int index) {
        int[] seat = seats.get(Math.min(index, seats.size() - 1));
        return new Vec3(seat[0] / 16.0, (seatHeight - HIPS_ABOVE_ATTACHMENT) / 16.0, -seat[1] / 16.0);
    }
}
