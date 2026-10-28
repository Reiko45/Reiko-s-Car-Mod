package com.reiko.cars.trailer;

import java.util.Locale;

/**
 * The kinds of semi trailer. They share a frame: 9 blocks long, the kingpin (which sits on a tractor's fifth wheel)
 * 3.5 blocks ahead of the middle and 18 pixels up, and two axles at the back. Sizes are in blocks unless they say
 * pixels, and match the models in {@code blockbench/}.
 */
public enum TrailerType {
    //              hitbox height  colour     cargo rows  tank buckets  car slots
    BOX_TRAILER(    3.75f,         0xF2F2F0,  6,          0,            0),
    TANKER_TRAILER( 3.8f,          0xC9CED4,  0,          64,           0),
    CAR_HAULER(     2.75f,         0x9C2A1E,  0,          0,            2);

    /** Length of every trailer, bumper to bumper. */
    public static final float LENGTH = 9.0f;
    /** Width of the hitbox. Trailers are longer than their box, so they're also picked by {@link TrailerPart}s. */
    public static final float WIDTH = 2.4f;
    /** How far the kingpin is ahead of the trailer's middle. */
    public static final double KINGPIN_FORWARD = 56 / 16.0;
    /** The kingpin's height above the ground: the height of a tractor's fifth wheel. */
    public static final double KINGPIN_HEIGHT = 18 / 16.0;
    /** How far behind the trailer's middle its axles are (midway between the two): the point the trailer pivots on. */
    public static final double AXLE_BACK = 51 / 16.0;
    /** The trailer can swing at most this far round from straight behind its tractor, in degrees. */
    public static final float MAX_ARTICULATION = 100.0f;
    /** Wheel radius, for spinning the wheels at the right rate. */
    public static final double WHEEL_RADIUS = 8 / 16.0;

    // The car hauler's deck, in pixels: its height, and where the middle of each car sits (ahead of the middle).
    public static final int DECK_HEIGHT = 22;
    private static final int[] CAR_SLOT_FORWARD = {18, -42};

    private final String id = name().toLowerCase(Locale.ROOT);
    public final float height;
    public final int defaultColor;
    public final int cargoRows;
    public final int tankBuckets;
    public final int carSlots;

    TrailerType(float height, int defaultColor, int cargoRows, int tankBuckets, int carSlots) {
        this.height = height;
        this.defaultColor = defaultColor;
        this.cargoRows = cargoRows;
        this.tankBuckets = tankBuckets;
        this.carSlots = carSlots;
    }

    /** The registry name, like {@code box_trailer}. */
    public String id() {
        return id;
    }

    /** How far ahead of the trailer's middle car slot {@code slot}'s car sits, in blocks. */
    public static double carSlotForward(int slot) {
        return CAR_SLOT_FORWARD[slot] / 16.0;
    }
}
