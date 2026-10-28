package com.reiko.cars.car;

import java.util.List;
import java.util.Locale;
import net.minecraft.world.phys.Vec3;

/**
 * The kinds of car and how each one drives. Speeds are in blocks per tick (1.0 is 72 km/h), fuel in BuildCraft MJ.
 *
 * <p>Seats are where each passenger's hips go, in model pixels (16 to a block) measured from the middle of the car
 * on the ground: {@code x} toward the car's left (the driver's side), {@code y} up, {@code z} toward the front. They
 * match the seats in the Blockbench models in {@code blockbench/}; move both together.
 */
public enum CarType {
    //          width  height length wheel  top    accel   brake   reverse turn  tank MJ/block colour  seat off-road bed
    SEDAN(       2.25f, 1.8f,  3.5f,  5,    0.90f, 0.012f, 0.040f, 0.25f, 4.0f,  8, 144, 0x2E5EAA,  7, false, 0,
            seat(6, -2), seat(-6, -2), seat(6, 9), seat(-6, 9)),
    SPORTS_CAR(  2.25f, 1.6f,  3.5f,  5,    1.30f, 0.020f, 0.050f, 0.25f, 4.5f,  6, 216, 0xC0262D,  5, false, 0,
            seat(6, 4), seat(-6, 4)),
    PICKUP_TRUCK(2.4f,  2.0f,  3.75f, 6,    0.80f, 0.010f, 0.035f, 0.22f, 3.5f, 12, 180, 0xE9E9E4, 10, false, 3,
            seat(6, -1), seat(-6, -1)),
    OFF_ROADER(  2.4f,  2.05f, 3.6f,  7,    0.85f, 0.014f, 0.040f, 0.25f, 4.0f, 10, 168, 0xC9A15A, 11, true,  0,
            seat(6, -3), seat(-6, -3), seat(6, 11), seat(-6, 11));

    /** MJ in a bucket of BuildCraft's light fuel. Tank sizes are given in buckets of it. */
    public static final double LIGHT_FUEL_MJ = 90_000;
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
    /** Rows of 9 slots in the truck bed, or 0 for none. */
    public final int storageRows;
    private final List<int[]> seats;

    CarType(float width, float height, float length, int wheelRadius, float topSpeed, float acceleration, float braking, float reverseSpeed,
            float turnRate, int tankBuckets, double fuelPerBlock, int defaultColor, int seatHeight, boolean offRoad,
            int storageRows, int[]... seats) {
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
        this.seats = List.of(seats);
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

    /** What the car runs on. Every car runs on gasoline for now; diesel pumps sell diesel by the bucket. */
    public FuelType fuel() {
        return FuelType.GASOLINE;
    }

    /** Fuel a full tank holds, in MJ. */
    public double tankCapacity() {
        return tankBuckets * LIGHT_FUEL_MJ;
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
