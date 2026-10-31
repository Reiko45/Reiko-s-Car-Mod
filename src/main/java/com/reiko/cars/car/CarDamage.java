package com.reiko.cars.car;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/**
 * Where a car's been hit, as it shows: how dented each side is (front, back, left and right, 0 to 1), the state of
 * each window (windshield, back window, left and right side windows: whole, cracked or shattered), and which of its
 * four lamp clusters (headlights and taillights, left and right, each with that corner's turn signal) are smashed.
 *
 * <p>The dents follow the body's condition: they come with the body damage a crash does, on the side it hit, and Body
 * Panels take them out as they mend the body. Windows are mended with Car Windows, one each, and lamps with Car Lights.
 */
public record CarDamage(float front, float rear, float left, float right, int windows, int lights) {
    public static final CarDamage NONE = new CarDamage(0.0F, 0.0F, 0.0F, 0.0F, 0, 0);

    /** A side of the car. */
    public enum Zone {
        FRONT, REAR, LEFT, RIGHT
    }

    // Windows, two bits each in {@link #windows}.
    public static final int WINDSHIELD = 0;
    public static final int BACK_WINDOW = 1;
    public static final int LEFT_WINDOW = 2;
    public static final int RIGHT_WINDOW = 3;
    public static final int WINDOWS = 4;
    public static final int WHOLE = 0;
    public static final int CRACKED = 1;
    public static final int SHATTERED = 2;

    // Lamp clusters, a bit each in {@link #lights}.
    public static final int FRONT_LEFT = 0;
    public static final int FRONT_RIGHT = 1;
    public static final int REAR_LEFT = 2;
    public static final int REAR_RIGHT = 3;
    public static final int LIGHTS = 4;

    /** Dent per unit of body condition a crash takes off, on the side it hit: a sedan into a wall flat out crumples its front most of the way. */
    static final float DENT_PER_BODY = 2.0F;
    /** Glass starts cracking at impacts over this (blocks per tick on that side: about 13 mph). */
    static final double GLASS_CRACKS = 0.3;
    /** And is sure to crack this much harder. */
    static final double GLASS_SPREAD = 0.35;
    /** Hits this hard can shatter a window outright (about 36 mph). */
    static final double GLASS_SHATTERS = 0.8;
    static final double LAMPS_BREAK = 0.25;
    static final double LAMPS_SPREAD = 0.35;
    /** Below this much difference between the sides, a car runs straight. */
    static final float PULL_FROM = 0.15F;
    /** Degrees a tick of pull, at the most. */
    static final float PULL = 1.0F;

    public static final Codec<CarDamage> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.FLOAT.optionalFieldOf("front", 0.0F).forGetter(CarDamage::front),
            Codec.FLOAT.optionalFieldOf("rear", 0.0F).forGetter(CarDamage::rear),
            Codec.FLOAT.optionalFieldOf("left", 0.0F).forGetter(CarDamage::left),
            Codec.FLOAT.optionalFieldOf("right", 0.0F).forGetter(CarDamage::right),
            Codec.INT.optionalFieldOf("windows", 0).forGetter(CarDamage::windows),
            Codec.INT.optionalFieldOf("lights", 0).forGetter(CarDamage::lights)
    ).apply(instance, CarDamage::new));

    public static final StreamCodec<ByteBuf, CarDamage> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, CarDamage::front,
            ByteBufCodecs.FLOAT, CarDamage::rear,
            ByteBufCodecs.FLOAT, CarDamage::left,
            ByteBufCodecs.FLOAT, CarDamage::right,
            ByteBufCodecs.VAR_INT, CarDamage::windows,
            ByteBufCodecs.VAR_INT, CarDamage::lights,
            CarDamage::new);

    public CarDamage {
        front = clamp(front);
        rear = clamp(rear);
        left = clamp(left);
        right = clamp(right);
        windows &= 0xFF;
        lights &= 0xF;
    }

    private static float clamp(float value) {
        return Float.isNaN(value) ? 0.0F : Mth.clamp(value, 0.0F, 1.0F);
    }

    public boolean isNone() {
        return front <= 0.0F && rear <= 0.0F && left <= 0.0F && right <= 0.0F && windows == 0 && lights == 0;
    }

    // ---- dents ----

    public float dent(Zone zone) {
        return switch (zone) {
            case FRONT -> front;
            case REAR -> rear;
            case LEFT -> left;
            case RIGHT -> right;
        };
    }

    public CarDamage withDent(Zone zone, float value) {
        return switch (zone) {
            case FRONT -> new CarDamage(value, rear, left, right, windows, lights);
            case REAR -> new CarDamage(front, value, left, right, windows, lights);
            case LEFT -> new CarDamage(front, rear, value, right, windows, lights);
            case RIGHT -> new CarDamage(front, rear, left, value, windows, lights);
        };
    }

    /** The worst dent. */
    public float worstDent() {
        return Math.max(Math.max(front, rear), Math.max(left, right));
    }

    /**
     * Mending the body from {@code before} to {@code after} takes the dents out in step: all of them once the body's as
     * good as new.
     */
    public CarDamage afterBodyRepair(float before, float after) {
        if (after <= before) {
            return this;
        }
        float scale = after >= 0.995F || before >= 1.0F ? 0.0F : (1.0F - after) / (1.0F - before);
        return new CarDamage(front * scale, rear * scale, left * scale, right * scale, windows, lights);
    }

    /**
     * How much it pulls to one side, in degrees a tick at speed, to the left if positive: toward its more battered
     * side, once one side's bent well beyond the other.
     */
    public float pull() {
        float difference = left - right;
        float magnitude = Math.abs(difference) - PULL_FROM;
        if (magnitude <= 0.0F || Math.max(left, right) < 0.3F) {
            return 0.0F;
        }
        return Math.signum(difference) * magnitude / (1.0F - PULL_FROM) * PULL;
    }

    // ---- windows ----

    /** Window {@code index}'s state: {@link #WHOLE}, {@link #CRACKED} or {@link #SHATTERED}. */
    public int window(int index) {
        return (windows >> (index * 2)) & 3;
    }

    public CarDamage withWindow(int index, int state) {
        int cleared = windows & ~(3 << (index * 2));
        return new CarDamage(front, rear, left, right, cleared | (Math.min(SHATTERED, state) << (index * 2)), lights);
    }

    /** How many windows are cracked or shattered. */
    public int damagedWindows() {
        int count = 0;
        for (int i = 0; i < WINDOWS; i++) {
            if (window(i) != WHOLE) {
                count++;
            }
        }
        return count;
    }

    /** The glass as a condition, for the repair bench: 1 whole, a cracked window a half off its share, a shattered one all of it. */
    public float glassCondition() {
        float lost = 0.0F;
        for (int i = 0; i < WINDOWS; i++) {
            lost += window(i) == SHATTERED ? 1.0F : window(i) == CRACKED ? 0.5F : 0.0F;
        }
        return 1.0F - lost / WINDOWS;
    }

    /** Fits {@code count} new windows, the worst first. */
    public CarDamage fixWindows(int count) {
        CarDamage fixed = this;
        for (int state = SHATTERED; state >= CRACKED && count > 0; state--) {
            for (int i = 0; i < WINDOWS && count > 0; i++) {
                if (fixed.window(i) == state) {
                    fixed = fixed.withWindow(i, WHOLE);
                    count--;
                }
            }
        }
        return fixed;
    }

    // ---- lamps ----

    public boolean lightBroken(int index) {
        return (lights & (1 << index)) != 0;
    }

    public CarDamage withLight(int index, boolean broken) {
        int changed = broken ? lights | (1 << index) : lights & ~(1 << index);
        return new CarDamage(front, rear, left, right, windows, changed);
    }

    public int brokenLights() {
        return Integer.bitCount(lights);
    }

    /** How many headlights still work: 2, 1 or 0. */
    public int workingHeadlights() {
        return (lightBroken(FRONT_LEFT) ? 0 : 1) + (lightBroken(FRONT_RIGHT) ? 0 : 1);
    }

    /** Whether a side's turn signal still has a lamp that works, front or back. */
    public boolean signalWorks(boolean leftSide) {
        return leftSide ? !lightBroken(FRONT_LEFT) || !lightBroken(REAR_LEFT) : !lightBroken(FRONT_RIGHT) || !lightBroken(REAR_RIGHT);
    }

    public float lightCondition() {
        return 1.0F - brokenLights() / (float) LIGHTS;
    }

    /** Fits {@code count} new lamp clusters, headlights first. */
    public CarDamage fixLights(int count) {
        CarDamage fixed = this;
        for (int i = 0; i < LIGHTS && count > 0; i++) {
            if (fixed.lightBroken(i)) {
                fixed = fixed.withLight(i, false);
                count--;
            }
        }
        return fixed;
    }

    // ---- crashes ----

    /** A side's share of a hit, from the cosine of its angle to it: none for a side facing away, or touched only by rounding (a right angle's cosine isn't quite 0). */
    private static float share(double cosine) {
        return cosine < 0.01 ? 0.0F : (float) cosine;
    }

    /**
     * After a crash at {@code impact} blocks per tick that took {@code bodyLost} off the body, coming from {@code angle}
     * radians round from straight ahead (positive to the car's left: a quarter turn is its left side, a half turn its
     * back): the sides it hit dented, in proportion, and maybe their glass cracked and lamps smashed.
     *
     * @param oneLamp a motorcycle: one headlight and one taillight, which break together
     */
    public CarDamage afterCrash(double impact, double angle, float bodyLost, boolean oneLamp, RandomSource random) {
        double ahead = Math.cos(angle);
        double side = Math.sin(angle);
        float frontShare = share(ahead);
        float rearShare = share(-ahead);
        float leftShare = share(side);
        float rightShare = share(-side);
        float dented = Math.max(0.0F, bodyLost) * DENT_PER_BODY;
        CarDamage after = new CarDamage(front + dented * frontShare, rear + dented * rearShare, left + dented * leftShare,
                right + dented * rightShare, windows, lights);
        // The glass on the sides it hit.
        after = after.hitWindow(WINDSHIELD, impact * frontShare, random)
                .hitWindow(BACK_WINDOW, impact * rearShare, random)
                .hitWindow(LEFT_WINDOW, impact * leftShare * 0.8, random)
                .hitWindow(RIGHT_WINDOW, impact * rightShare * 0.8, random);
        // The lamps at the corners it hit: the end's two, and the side's two, a little less.
        double[] force = {
                impact * Math.max(frontShare, leftShare * 0.7),
                impact * Math.max(frontShare, rightShare * 0.7),
                impact * Math.max(rearShare, leftShare * 0.7),
                impact * Math.max(rearShare, rightShare * 0.7)};
        for (int i = 0; i < LIGHTS; i++) {
            if (!after.lightBroken(i) && random.nextDouble() < chance(force[i], LAMPS_BREAK, LAMPS_SPREAD)) {
                after = after.withLight(i, true);
            }
        }
        if (oneLamp) {
            // A motorcycle's lamp is one, down the middle.
            boolean head = after.lightBroken(FRONT_LEFT) || after.lightBroken(FRONT_RIGHT);
            boolean tail = after.lightBroken(REAR_LEFT) || after.lightBroken(REAR_RIGHT);
            after = after.withLight(FRONT_LEFT, head).withLight(FRONT_RIGHT, head).withLight(REAR_LEFT, tail).withLight(REAR_RIGHT, tail);
        }
        return after;
    }

    private CarDamage hitWindow(int index, double force, RandomSource random) {
        int state = window(index);
        if (state == SHATTERED || force <= GLASS_CRACKS) {
            return this;
        }
        if (force > GLASS_SHATTERS && random.nextDouble() < chance(force, GLASS_SHATTERS, 0.4)) {
            return withWindow(index, SHATTERED);
        }
        if (random.nextDouble() < chance(force, GLASS_CRACKS, GLASS_SPREAD)) {
            return withWindow(index, state + 1); // whole cracks; cracked shatters
        }
        return this;
    }

    /** No chance at {@code from}, rising to certain {@code spread} past it. */
    private static double chance(double force, double from, double spread) {
        return Mth.clamp((force - from) / spread, 0.0, 1.0);
    }
}
