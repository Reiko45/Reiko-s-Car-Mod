package com.reiko.cars.car;

import static com.reiko.cars.car.FuelType.DIESEL;
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
 * <p>Tank sizes are in buckets of the car's own fuel: light fuel for gasoline cars, dense fuel for diesel ones. A
 * tractor's fifth wheel, where a trailer's kingpin sits, is given in pixels behind the middle and up from the ground.
 */
public enum CarType {
    //          width  height length wheel  top    accel   brake   reverse turn  tank MJ/block colour  seat off-road bed wheels fuel   fifth wheel
    SEDAN(       2.25f, 1.8f,  3.5f,  5,    0.90f, 0.012f, 0.040f, 0.25f, 4.0f,  8, 144, 0x2E5EAA,  7, false, 0, 4, GASOLINE, noFifthWheel(),
            seat(6, -2), seat(-6, -2), seat(6, 9), seat(-6, 9)),
    SPORTS_CAR(  2.25f, 1.6f,  3.5f,  5,    1.30f, 0.020f, 0.050f, 0.25f, 4.5f,  6, 216, 0xC0262D,  5, false, 0, 4, GASOLINE, noFifthWheel(),
            seat(6, 4), seat(-6, 4)),
    PICKUP_TRUCK(2.4f,  2.0f,  3.75f, 6,    0.80f, 0.010f, 0.035f, 0.22f, 3.5f, 12, 180, 0xE9E9E4, 10, false, 3, 4, GASOLINE, noFifthWheel(),
            seat(6, -1), seat(-6, -1)),
    OFF_ROADER(  2.4f,  2.05f, 3.6f,  7,    0.85f, 0.014f, 0.040f, 0.25f, 4.0f, 10, 168, 0xC9A15A, 11, true,  0, 4, GASOLINE, noFifthWheel(),
            seat(6, -3), seat(-6, -3), seat(6, 11), seat(-6, 11)),
    /** A semi tractor: pulls trailers on the fifth wheel behind its sleeper. Its tank is in buckets of dense fuel. */
    SEMI_TRUCK(  2.4f,  3.6f,  6.25f, 8,    0.85f, 0.010f, 0.035f, 0.20f, 3.2f, 12, 1440, 0x2E6B3A, 25, false, 0, 4, DIESEL, fifthWheel(32, 18),
            seat(9, -12), seat(-9, -12)),

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
            seat(0, 6));

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

    /** What it runs on: gasoline, or diesel for the semi. */
    public FuelType fuel() {
        return fuel;
    }

    /** Fuel a full tank holds, in MJ. */
    public double tankCapacity() {
        return tankBuckets * fuel.bucketEnergy;
    }

    /** A motorcycle: two wheels, leans into turns. */
    public boolean isBike() {
        return wheels == 2;
    }

    /** What its storage is called: the pickup's truck bed, the cruiser's saddlebags. */
    public String storageKey() {
        return isBike() ? "container.reikocars.saddlebags" : "container.reikocars.truck_bed";
    }

    /** The horn's pitch: a big truck's is low, a scooter's beeps. */
    public float hornPitch() {
        return switch (this) {
            case SPORTS_CAR -> 1.15F;
            case PICKUP_TRUCK -> 0.85F;
            case SEMI_TRUCK -> 0.6F;
            case SPORT_BIKE, DIRT_BIKE -> 1.35F;
            case CRUISER -> 1.1F;
            case SCOOTER -> 1.6F;
            default -> 1.0F;
        };
    }

    /** The engine sound's pitch at idle, against a car's 1.0. */
    public float enginePitch() {
        return switch (this) {
            case SEMI_TRUCK -> 0.75F;
            case SPORT_BIKE -> 1.3F;
            case CRUISER -> 0.8F;
            case DIRT_BIKE -> 1.4F;
            case SCOOTER -> 1.6F;
            default -> 1.0F;
        };
    }

    /** Whether it can pull a trailer. */
    public boolean isTractor() {
        return fifthWheel != null;
    }

    /**
     * Where a trailer's kingpin sits, relative to the car's position (the middle of its footprint, on the ground) when
     * it faces south (+z), in blocks. Rotate it by the car's yaw before use. Only for tractors.
     */
    /**
     * How far behind its middle the vehicle turns about, in blocks. A tractor turns about its back axles, which are
     * under the fifth wheel, as a real one does: then the fifth wheel follows the cab round a bend instead of being
     * flung out sideways, and the trailer follows it. Cars turn about their middle.
     */
    public double pivotBack() {
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
