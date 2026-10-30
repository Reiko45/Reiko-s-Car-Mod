package com.reiko.cars.tuning;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * What's been done to a car at a Tuning Bench. Each upgrade makes it quicker somewhere and costs it somewhere else:
 *
 * <ul>
 *   <li><b>Engine</b> stages 1 to 3: top speed +6% a stage (and +4% pull), but 8% more fuel and 15% more engine wear;</li>
 *   <li><b>Turbo</b>: acceleration +25%, 10% more fuel and engine wear;</li>
 *   <li><b>Sport tires</b>: turn 15% sharper, but slower off-road and they wear 30% faster; <b>off-road tires</b>: much
 *       better off-road, 3% off the top speed;</li>
 *   <li><b>Brakes</b> stages 1 and 2: stop 20% harder a stage;</li>
 *   <li><b>Lowered</b>: turn 10% sharper, worse off-road; <b>lifted</b>: better off-road, turns 5% wider;</li>
 *   <li><b>Fuel tank</b> stages 1 and 2: holds 25% more a stage, but the extra weight takes 3% off acceleration a stage
 *       (gasoline and diesel cars only).</li>
 * </ul>
 */
public record Upgrades(int engine, boolean turbo, Tires tires, int brakes, Suspension suspension, int tank) {
    public static final int MAX_ENGINE = 3;
    public static final int MAX_BRAKES = 2;
    public static final int MAX_TANK = 2;
    public static final Upgrades STOCK = new Upgrades(0, false, Tires.STOCK, 0, Suspension.STOCK, 0);

    public enum Tires { STOCK, SPORT, OFF_ROAD }

    public enum Suspension { STOCK, LOWERED, LIFTED }

    public static final Codec<Upgrades> CODEC = Codec.INT.xmap(Upgrades::unpack, Upgrades::pack);
    public static final StreamCodec<ByteBuf, Upgrades> STREAM_CODEC = ByteBufCodecs.VAR_INT.map(Upgrades::unpack, Upgrades::pack);

    public Upgrades {
        engine = Math.max(0, Math.min(MAX_ENGINE, engine));
        brakes = Math.max(0, Math.min(MAX_BRAKES, brakes));
        tank = Math.max(0, Math.min(MAX_TANK, tank));
    }

    /** All of it in one int, for syncing and saving. */
    public int pack() {
        return engine | (turbo ? 1 : 0) << 2 | tires.ordinal() << 3 | brakes << 5 | suspension.ordinal() << 7 | tank << 9;
    }

    public static Upgrades unpack(int packed) {
        Tires[] tires = Tires.values();
        Suspension[] suspension = Suspension.values();
        return new Upgrades(packed & 3, (packed >> 2 & 1) != 0, tires[Math.min(tires.length - 1, packed >> 3 & 3)], packed >> 5 & 3,
                suspension[Math.min(suspension.length - 1, packed >> 7 & 3)], packed >> 9 & 3);
    }

    public boolean isStock() {
        return equals(STOCK);
    }

    public Upgrades withEngine(int stage) {
        return new Upgrades(stage, turbo, tires, brakes, suspension, tank);
    }

    public Upgrades withTurbo(boolean on) {
        return new Upgrades(engine, on, tires, brakes, suspension, tank);
    }

    public Upgrades withTires(Tires fitted) {
        return new Upgrades(engine, turbo, fitted, brakes, suspension, tank);
    }

    public Upgrades withBrakes(int stage) {
        return new Upgrades(engine, turbo, tires, stage, suspension, tank);
    }

    public Upgrades withSuspension(Suspension fitted) {
        return new Upgrades(engine, turbo, tires, brakes, fitted, tank);
    }

    public Upgrades withTank(int stage) {
        return new Upgrades(engine, turbo, tires, brakes, suspension, stage);
    }

    // ---- what they do ----

    public double topSpeed() {
        return (1.0 + 0.06 * engine) * (tires == Tires.OFF_ROAD ? 0.97 : 1.0);
    }

    public double acceleration() {
        return (1.0 + 0.04 * engine) * (turbo ? 1.25 : 1.0) * (1.0 - 0.03 * tank);
    }

    public double braking() {
        return 1.0 + 0.2 * brakes;
    }

    public double turning() {
        return (tires == Tires.SPORT ? 1.15 : 1.0) * (suspension == Suspension.LOWERED ? 1.1 : suspension == Suspension.LIFTED ? 0.95 : 1.0);
    }

    /** The share of its top speed a car keeps off-road ({@code factor} as it comes), with these tires and springs. */
    public double offRoad(double factor) {
        double f = factor;
        if (tires == Tires.OFF_ROAD) {
            f += (1.0 - f) * 0.6;
        } else if (tires == Tires.SPORT) {
            f *= 0.85;
        }
        if (suspension == Suspension.LIFTED) {
            f += (1.0 - f) * 0.3;
        } else if (suspension == Suspension.LOWERED) {
            f *= 0.85;
        }
        return f;
    }

    /** How much more the fuel tank holds than it came with: 1.25 a stage. */
    public double tankSize() {
        return 1.0 + 0.25 * tank;
    }

    public double fuelUse() {
        return 1.0 + 0.08 * engine + (turbo ? 0.1 : 0.0);
    }

    public double engineWear() {
        return 1.0 + 0.15 * engine + (turbo ? 0.1 : 0.0);
    }

    public double tireWear() {
        return tires == Tires.SPORT ? 1.3 : 1.0;
    }

    /** What's fitted, a line each: "Stage 2 engine", "Turbo", ... (nothing for a stock car). */
    public List<Component> describe() {
        List<Component> lines = new ArrayList<>();
        if (engine > 0) {
            lines.add(Component.translatable("tuning.reikocars.engine_stage", engine));
        }
        if (turbo) {
            lines.add(Component.translatable("tuning.reikocars.turbo"));
        }
        if (tires != Tires.STOCK) {
            lines.add(Component.translatable("tuning.reikocars.tires." + tires.name().toLowerCase(Locale.ROOT)));
        }
        if (brakes > 0) {
            lines.add(Component.translatable("tuning.reikocars.brakes_stage", brakes));
        }
        if (suspension != Suspension.STOCK) {
            lines.add(Component.translatable("tuning.reikocars.suspension." + suspension.name().toLowerCase(Locale.ROOT)));
        }
        if (tank > 0) {
            lines.add(Component.translatable("tuning.reikocars.tank_stage", tank, Math.round(25.0 * tank)));
        }
        return lines;
    }
}
