package com.reiko.cars.gps;

import java.util.Locale;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** The GPS's sums: which way the arrow points, and how far and how long to go, in words. */
public final class GpsMath {
    private static final String[] COMPASS = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};
    private static final double FEET = 3.28084;

    private GpsMath() {
    }

    /** The way someone looking at {@code yaw} degrees is facing, flat. */
    public static Vec3 forward(float yaw) {
        float radians = yaw * Mth.DEG_TO_RAD;
        return new Vec3(-Mth.sin(radians), 0.0, Mth.cos(radians));
    }

    /**
     * How far round from {@code forward} the way to go is, flat: 0 straight ahead, 90 to the right, -90 to the left,
     * 180 behind, in degrees.
     */
    public static double relativeAngle(Vec3 forward, double dx, double dz) {
        // (Looking down on the world, with east +x and south +z, the right of facing (fx, fz) is (-fz, fx).)
        double ahead = dx * forward.x + dz * forward.z;
        double right = dx * -forward.z + dz * forward.x;
        if (ahead == 0.0 && right == 0.0) {
            return 0.0;
        }
        return Math.toDegrees(Math.atan2(right, ahead));
    }

    /** "N", "NE" and so on: the way from here to there, {@code dx} east and {@code dz} south. */
    public static String compass(double dx, double dz) {
        double bearing = Math.toDegrees(Math.atan2(dx, -dz)); // clockwise from north
        int sector = (int) Math.floor(((bearing + 360.0 + 22.5) % 360.0) / 45.0);
        return COMPASS[sector % 8];
    }

    /** A distance in blocks (meters), as "85 m" or "1.2 km", or in feet and miles. */
    public static String distance(double blocks, boolean metric) {
        if (metric) {
            if (blocks < 100.0) {
                return Math.round(blocks) + " m";
            }
            if (blocks < 1000.0) {
                return Math.round(blocks / 10.0) * 10 + " m";
            }
            return String.format(Locale.ROOT, "%.1f km", blocks / 1000.0);
        }
        double feet = blocks * FEET;
        if (feet < 1000.0) {
            return Math.round(feet / 10.0) * 10 + " ft";
        }
        return String.format(Locale.ROOT, "%.1f mi", feet / 5280.0);
    }

    /** How long, as "0:45", "12:05" or "1 h 05". */
    public static String duration(double seconds) {
        long total = Math.max(0, Math.round(seconds));
        if (total >= 3600) {
            return String.format(Locale.ROOT, "%d h %02d", total / 3600, (total % 3600) / 60);
        }
        return String.format(Locale.ROOT, "%d:%02d", total / 60, total % 60);
    }

    /** Whether someone {@code dx}, {@code dy}, {@code dz} from a place is there: close by, and not far above or below it. */
    public static boolean arrived(double dx, double dy, double dz, boolean driving) {
        double reach = driving ? 12.0 : 4.0;
        return dx * dx + dz * dz <= reach * reach && Math.abs(dy) <= 16.0;
    }
}
