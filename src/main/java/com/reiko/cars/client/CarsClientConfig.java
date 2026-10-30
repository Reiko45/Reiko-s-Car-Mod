package com.reiko.cars.client;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Per-player settings, in {@code config/reikocars-client.toml}. */
public final class CarsClientConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.EnumValue<SpeedUnit> SPEED_UNIT;
    public static final ModConfigSpec.BooleanValue SHOW_HUD;
    public static final ModConfigSpec.BooleanValue GPS_ON_MINIMAP;
    public static final ModConfigSpec.BooleanValue CARS_ON_MINIMAP;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        SPEED_UNIT = builder.comment("Speedometer units: MPH or KMH.").defineEnum("speedUnit", SpeedUnit.MPH);
        SHOW_HUD = builder.comment("Show the speedometer and fuel gauge while in a car.").define("showHud", true);
        builder.comment("With Xaero's Minimap installed.").push("minimap");
        GPS_ON_MINIMAP = builder.comment("Show where the GPS is taking you as a waypoint on the minimap, in the world and on the world map.")
                .define("gpsDestination", true);
        CARS_ON_MINIMAP = builder.comment("Show your cars (and any you're renting) as waypoints on the minimap and in the world.")
                .define("yourCars", true);
        builder.pop();
        SPEC = builder.build();
    }

    private CarsClientConfig() {
    }

    public enum SpeedUnit {
        MPH(44.7387, "hud.reikocars.mph"),
        KMH(72.0, "hud.reikocars.kmh");

        /** Multiplies blocks per tick (a block is a metre, a tick a twentieth of a second). */
        public final double perBlockPerTick;
        public final String translationKey;

        SpeedUnit(double perBlockPerTick, String translationKey) {
            this.perBlockPerTick = perBlockPerTick;
            this.translationKey = translationKey;
        }
    }

    public static SpeedUnit speedUnit() {
        return SPEC.isLoaded() ? SPEED_UNIT.get() : SpeedUnit.MPH;
    }

    public static boolean showHud() {
        return !SPEC.isLoaded() || SHOW_HUD.get();
    }

    public static boolean gpsOnMinimap() {
        return !SPEC.isLoaded() || GPS_ON_MINIMAP.get();
    }

    public static boolean carsOnMinimap() {
        return !SPEC.isLoaded() || CARS_ON_MINIMAP.get();
    }
}
