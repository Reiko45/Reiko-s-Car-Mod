package com.reiko.cars.trailer;

import java.util.Locale;

/**
 * The kinds of trailer. The semi's trailers share a frame: 9 blocks long, the kingpin (which sits on a tractor's fifth
 * wheel) 3.5 blocks ahead of the middle and 18 pixels up, and two axles at the back. The bumper-pull trailers (the
 * camper and the livestock trailer) are shorter, and hitch to a tow ball behind a pickup or SUV: their coupler sits on
 * the end of an A-frame tongue ahead of the body, 10 pixels up. Sizes are in blocks unless they say pixels, and match
 * the models in {@code blockbench/}. A trailer's "middle" is the middle of its body; for a bumper-pull, the tongue
 * reaches out ahead of that.
 */
public enum TrailerType {
    //                       hitch        body length width  height colour    cargo rows tank  cars  stock rows camper
    BOX_TRAILER(             Hitch.FIFTH_WHEEL, 9.0f, 2.4f,  3.75f, 0xF2F2F0, 6,         0,    0,    0,         false),
    TANKER_TRAILER(          Hitch.FIFTH_WHEEL, 9.0f, 2.4f,  3.8f,  0xC9CED4, 0,         64,   0,    0,         false),
    CAR_HAULER(              Hitch.FIFTH_WHEEL, 9.0f, 2.4f,  2.75f, 0x9C2A1E, 0,         0,    2,    0,         false),
    /** A travel trailer: a bed, cupboards and a stove, and a camp that unfolds when it's parked. */
    CAMPER(                  Hitch.BALL,        5.5f, 2.2f,  3.4f,  0xF4F1E8, 3,         0,    0,    0,         true),
    /** A stock trailer: farm animals led up its ramp ride inside, seen between the slats. */
    LIVESTOCK_TRAILER(       Hitch.BALL,        5.0f, 2.1f,  2.75f, 0xB9BFC6, 0,         0,    0,    3,         false),
    /** The camper's big brother, for the semi: a bedroom over the fifth wheel and a slide-out. */
    FIFTH_WHEEL_CAMPER(      Hitch.FIFTH_WHEEL, 9.0f, 2.4f,  4.15f, 0xF4F1E8, 5,         0,    0,    0,         true),
    /** A cattle trailer for the semi: twice the livestock trailer's pens. */
    SEMI_LIVESTOCK_TRAILER(  Hitch.FIFTH_WHEEL, 9.0f, 2.4f,  3.75f, 0xC9CED4, 0,         0,    0,    6,         false);

    /** What a trailer hitches to. */
    public enum Hitch {
        /** A semi's fifth wheel: the trailer's kingpin sits on it. */
        FIFTH_WHEEL,
        /** A tow ball behind a pickup or SUV: the trailer's coupler sits on it. */
        BALL
    }

    // The semi trailers' frame, in pixels: the kingpin's distance ahead of the middle and height, the axles' distance
    // behind it (midway between the two), the wheels' radius.
    private static final int KINGPIN_FORWARD = 56;
    private static final int KINGPIN_HEIGHT = 18;
    private static final int SEMI_AXLE_BACK = 51;
    private static final int SEMI_WHEEL_RADIUS = 8;
    /** A tow ball's height, in pixels: every pickup and SUV's ball is at this height, so any bumper-pull fits any. */
    public static final int BALL_HEIGHT = 10;
    /** The longest trailer's length: how far out to look for trailers whose body may reach something. */
    public static final float MAX_LENGTH = 9.0f;

    // The car hauler's deck, in pixels: its height, and where the middle of each car sits (ahead of the middle).
    public static final int DECK_HEIGHT = 22;
    private static final int[] CAR_SLOT_FORWARD = {18, -42};

    private final String id = name().toLowerCase(Locale.ROOT);
    public final Hitch hitch;
    /** Length of the body, bumper to bumper (a bumper-pull's tongue reaches out ahead of it). */
    public final float length;
    /** Width of the hitbox. Trailers are longer than their box, so they're also picked by {@link TrailerPart}s. */
    public final float width;
    public final float height;
    public final int defaultColor;
    /** Rows of nine cargo slots: a box trailer's hold, a camper's cupboards. */
    public final int cargoRows;
    public final int tankBuckets;
    public final int carSlots;
    /** Rows of pens for farm animals, each with room for two cows, one horse or four chickens. */
    public final int stockRows;
    /** A camper: a bed, cupboards, a stove, and a camp that unfolds when it's parked. */
    public final boolean camper;

    TrailerType(Hitch hitch, float length, float width, float height, int defaultColor, int cargoRows, int tankBuckets, int carSlots,
            int stockRows, boolean camper) {
        this.hitch = hitch;
        this.length = length;
        this.width = width;
        this.height = height;
        this.defaultColor = defaultColor;
        this.cargoRows = cargoRows;
        this.tankBuckets = tankBuckets;
        this.carSlots = carSlots;
        this.stockRows = stockRows;
        this.camper = camper;
    }

    /** The registry name, like {@code box_trailer}. */
    public String id() {
        return id;
    }

    /** A semi trailer, which hitches to a fifth wheel. */
    public boolean isSemiTrailer() {
        return hitch == Hitch.FIFTH_WHEEL;
    }

    /** Carries farm animals. */
    public boolean carriesLivestock() {
        return stockRows > 0;
    }

    /** Freight: a trailer whose load pays the Trucker job (not a camper). */
    public boolean isFreight() {
        return !camper;
    }

    /** How far the coupler (or kingpin) is ahead of the trailer's middle. */
    public double couplerForward() {
        return switch (this) {
            case CAMPER -> 64 / 16.0;
            case LIVESTOCK_TRAILER -> 58 / 16.0;
            default -> KINGPIN_FORWARD / 16.0;
        };
    }

    /** The coupler's height above the ground: the height of the fifth wheel or tow ball it sits on. */
    public double couplerHeight() {
        return (hitch == Hitch.BALL ? BALL_HEIGHT : KINGPIN_HEIGHT) / 16.0;
    }

    /** How far behind the trailer's middle its axles are (midway between the two): the point the trailer pivots on. */
    public double axleBack() {
        return switch (this) {
            case CAMPER -> 11 / 16.0;
            case LIVESTOCK_TRAILER -> 9 / 16.0;
            default -> SEMI_AXLE_BACK / 16.0;
        };
    }

    /** Wheel radius, for spinning the wheels at the right rate. */
    public double wheelRadius() {
        return switch (this) {
            case CAMPER, LIVESTOCK_TRAILER -> 6 / 16.0;
            case FIFTH_WHEEL_CAMPER -> 7 / 16.0;
            default -> SEMI_WHEEL_RADIUS / 16.0;
        };
    }

    /** The trailer can swing at most this far round from straight behind what's pulling it, in degrees. */
    public float maxArticulation() {
        return hitch == Hitch.BALL ? 85.0F : 100.0F;
    }

    /** The width (and depth) of each of the trailer's two end parts, which cover its length beyond the hitbox. */
    public float partWidth() {
        return isSemiTrailer() ? 3.2F : width;
    }

    /** How far ahead of and behind the middle the two end parts are. */
    public double partOffset() {
        return isSemiTrailer() ? 3.0 : length / 2.0 - width / 2.0;
    }

    /** How far ahead of the trailer's middle car slot {@code slot}'s car sits, in blocks. */
    public static double carSlotForward(int slot) {
        return CAR_SLOT_FORWARD[slot] / 16.0;
    }

    /** The farm animal pens' floor, in pixels above the ground. */
    public int stockFloor() {
        return this == LIVESTOCK_TRAILER ? 14 : 24;
    }

    /** The pens' length, front to back, and where the front one starts (ahead of the middle), in pixels. */
    public int[] stockSpan() {
        return this == LIVESTOCK_TRAILER ? new int[] {74, 37} : new int[] {136, 68};
    }

    /** The rear ramp's length, in pixels (it's the back gate, folded up, while travelling). */
    public int rampLength() {
        return this == LIVESTOCK_TRAILER ? 26 : 34;
    }
}
