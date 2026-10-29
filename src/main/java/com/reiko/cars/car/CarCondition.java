package com.reiko.cars.car;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

/**
 * What wear and tear has done to a car, part by part, each from 1 (as new) down to 0 (worn out).
 *
 * <ul>
 *   <li><b>Oil</b> runs down with distance. Low oil doesn't slow the car, but the engine wears four times as fast
 *       below a quarter, and twenty times as fast with none.</li>
 *   <li><b>Engine</b> wears slowly with distance, and a big crash knocks it about. A tired engine (below half) has
 *       less power, and any wear makes it burn more fuel.</li>
 *   <li><b>Tires</b> wear with distance, faster off the road and when the car is pulling a trailer, and with hard
 *       braking and handbrake skids. Bald tires (below 30%) grip less, so the car accelerates, brakes and turns worse.</li>
 *   <li><b>Brakes</b> wear with every stop, by how much speed they take off. Worn brakes (below 40%) stop the car
 *       more slowly.</li>
 *   <li><b>Body</b> only suffers in crashes, by how hard the car hits. A battered body (below half) slows it down.</li>
 * </ul>
 *
 * <p>Any part at 0 breaks the car down: the engine won't run until it's repaired, though the brakes still work. Wear
 * rates here are at the server's wear setting of 1.
 */
public record CarCondition(float oil, float engine, float tires, float brakes, float body) {
    public static final CarCondition NEW = new CarCondition(1.0F, 1.0F, 1.0F, 1.0F, 1.0F);

    public static final Codec<CarCondition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.FLOAT.optionalFieldOf("oil", 1.0F).forGetter(CarCondition::oil),
            Codec.FLOAT.optionalFieldOf("engine", 1.0F).forGetter(CarCondition::engine),
            Codec.FLOAT.optionalFieldOf("tires", 1.0F).forGetter(CarCondition::tires),
            Codec.FLOAT.optionalFieldOf("brakes", 1.0F).forGetter(CarCondition::brakes),
            Codec.FLOAT.optionalFieldOf("body", 1.0F).forGetter(CarCondition::body)
    ).apply(instance, CarCondition::new));

    public static final StreamCodec<ByteBuf, CarCondition> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, CarCondition::oil,
            ByteBufCodecs.FLOAT, CarCondition::engine,
            ByteBufCodecs.FLOAT, CarCondition::tires,
            ByteBufCodecs.FLOAT, CarCondition::brakes,
            ByteBufCodecs.FLOAT, CarCondition::body,
            CarCondition::new);

    // Blocks driven for a part to wear from new to nothing.
    static final double OIL_LIFE = 50_000;
    static final double ENGINE_LIFE = 400_000;
    static final double TIRE_LIFE = 80_000;
    /** Brake wear per block-per-tick of speed the brakes take off: about 550 stops from a sedan's top speed. */
    static final double BRAKE_WEAR = 0.002;
    /** Tire wear per block-per-tick of speed lost braking (a skid counts three times). */
    static final double BRAKE_TIRE_WEAR = 0.0003;
    /** Crashes slower than this (blocks per tick, about 7 mph) leave no mark. */
    public static final double CRASH_THRESHOLD = 0.15;
    /** Body damage per block-per-tick of impact over the threshold: a sedan into a wall at full speed takes 45%. */
    static final double CRASH_DAMAGE = 0.6;
    /** Impacts harder than this (about 27 mph) damage the engine too. */
    static final double ENGINE_CRASH_THRESHOLD = 0.6;

    public CarCondition {
        oil = clamp(oil);
        engine = clamp(engine);
        tires = clamp(tires);
        brakes = clamp(brakes);
        body = clamp(body);
    }

    private static float clamp(float value) {
        return Float.isNaN(value) ? 1.0F : Mth.clamp(value, 0.0F, 1.0F);
    }

    /** The parts that wear, in the order the dashboard and repair bench show them. */
    public enum Part {
        OIL, ENGINE, TIRES, BRAKES, BODY;

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }

        public Component displayName() {
            return Component.translatable("condition.reikocars." + id());
        }

        public static Part byIndex(int index) {
            Part[] parts = values();
            return parts[Mth.clamp(index, 0, parts.length - 1)];
        }
    }

    /** How worn a part is, for the dashboard's warning lights. */
    public enum Level {
        GOOD, LOW, CRITICAL, BROKEN;

        public static Level of(float value) {
            if (value <= 0.0F) {
                return BROKEN;
            }
            if (value <= 0.10F) {
                return CRITICAL;
            }
            return value <= 0.25F ? LOW : GOOD;
        }

        public ChatFormatting color() {
            return switch (this) {
                case GOOD -> ChatFormatting.GREEN;
                case LOW -> ChatFormatting.YELLOW;
                case CRITICAL -> ChatFormatting.GOLD;
                case BROKEN -> ChatFormatting.RED;
            };
        }
    }

    public float get(Part part) {
        return switch (part) {
            case OIL -> oil;
            case ENGINE -> engine;
            case TIRES -> tires;
            case BRAKES -> brakes;
            case BODY -> body;
        };
    }

    public CarCondition with(Part part, float value) {
        return switch (part) {
            case OIL -> new CarCondition(value, engine, tires, brakes, body);
            case ENGINE -> new CarCondition(oil, value, tires, brakes, body);
            case TIRES -> new CarCondition(oil, engine, value, brakes, body);
            case BRAKES -> new CarCondition(oil, engine, tires, value, body);
            case BODY -> new CarCondition(oil, engine, tires, brakes, value);
        };
    }

    public Level level(Part part) {
        return Level.of(get(part));
    }

    /** The first worn-out part, which keeps the car from running; null while it runs. */
    @Nullable
    public Part brokenPart() {
        for (Part part : Part.values()) {
            if (get(part) <= 0.0F) {
                return part;
            }
        }
        return null;
    }

    /** The most worn part. */
    public Part worstPart() {
        Part worst = Part.OIL;
        for (Part part : Part.values()) {
            if (get(part) < get(worst)) {
                worst = part;
            }
        }
        return worst;
    }

    /** All the parts averaged, 0 to 1. */
    public float overall() {
        return (oil + engine + tires + brakes + body) / 5.0F;
    }

    public boolean isNew() {
        return oil >= 1.0F && engine >= 1.0F && tires >= 1.0F && brakes >= 1.0F && body >= 1.0F;
    }

    /** Whether any part differs from {@code other}'s by at least {@code step}, or is at a different warning level. */
    public boolean differs(CarCondition other, float step) {
        for (Part part : Part.values()) {
            if (Math.abs(get(part) - other.get(part)) >= step || level(part) != other.level(part)) {
                return true;
            }
        }
        return false;
    }

    // ---- wear ----

    /**
     * After driving {@code blocks}.
     *
     * @param tireFactor how hard the surface is on tires: 1 on a road, more off it
     * @param load how much harder a trailer makes things: 1 with none
     * @param rate the server's wear setting
     */
    public CarCondition afterDriving(double blocks, double tireFactor, double load, double rate) {
        if (blocks <= 0 || rate <= 0) {
            return this;
        }
        double newOil = oil - blocks / OIL_LIFE * rate;
        double engineRate = oil <= 0.0F ? 20.0 : oil < 0.25F ? 4.0 : 1.0;
        double newEngine = engine - blocks / ENGINE_LIFE * engineRate * load * rate;
        double newTires = tires - blocks / TIRE_LIFE * tireFactor * load * rate;
        return new CarCondition((float) newOil, (float) newEngine, (float) newTires, brakes, body);
    }

    /**
     * After the brakes took {@code speedLost} (blocks per tick, summed over the stops) off the car's speed, of which
     * {@code skidded} was with the wheels locked by the handbrake.
     */
    public CarCondition afterBraking(double speedLost, double skidded, double load, double rate) {
        if (rate <= 0 || (speedLost <= 0 && skidded <= 0)) {
            return this;
        }
        double newBrakes = brakes - Math.max(0, speedLost) * BRAKE_WEAR * load * rate;
        double newTires = tires - (Math.max(0, speedLost) + 3.0 * Math.max(0, skidded)) * BRAKE_TIRE_WEAR * load * rate;
        return new CarCondition(oil, engine, (float) newTires, (float) newBrakes, body);
    }

    /** After hitting something at {@code impact} blocks per tick. */
    public CarCondition afterCrash(double impact, double damage) {
        if (impact <= CRASH_THRESHOLD || damage <= 0) {
            return this;
        }
        double newBody = body - (impact - CRASH_THRESHOLD) * CRASH_DAMAGE * damage;
        double newEngine = engine - Math.max(0, impact - ENGINE_CRASH_THRESHOLD) * 0.25 * damage;
        return new CarCondition(oil, (float) newEngine, tires, brakes, (float) newBody);
    }

    /** After running into a mob at {@code speed} blocks per tick: a small dent, a percent or three. */
    public CarCondition afterHitting(double speed, double damage) {
        if (damage <= 0) {
            return this;
        }
        return with(Part.BODY, (float) (body - (0.01 + 0.03 * speed) * damage));
    }

    // ---- effects, used by whichever side drives the car ----

    /** Multiplies top speed and acceleration: a tired engine (below half) has less power, down to 60%. */
    public double power() {
        return engine >= 0.5F ? 1.0 : 0.6 + 0.8 * engine;
    }

    /** Multiplies the fuel burned: a worn-out engine burns half as much again. */
    public double fuelUse() {
        return 1.0 + 0.5 * (1.0 - engine);
    }

    /** Multiplies how hard the brakes stop the car: worn brakes (below 40%) down to 35%. */
    public double braking() {
        return brakes >= 0.4F ? 1.0 : 0.35 + 0.65 * (brakes / 0.4);
    }

    /** Multiplies the tires' grip: bald tires (below 30%) down to 55%. */
    public double grip() {
        return tires >= 0.3F ? 1.0 : 0.55 + 0.45 * (tires / 0.3);
    }

    /** Multiplies top speed: a battered body (below half) down to 75%. */
    public double drag() {
        return body >= 0.5F ? 1.0 : 0.75 + 0.5 * body;
    }

    /** "73%". */
    public static String percent(float value) {
        return Math.round(value * 100) + "%";
    }
}
