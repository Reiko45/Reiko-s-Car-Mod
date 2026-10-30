package com.reiko.cars.trailer;

import java.util.List;

/**
 * Where things are on a camper, in the model's pixels (x out to the curb side, where the door and awning are; y up
 * from the ground; z toward the back, from the middle of the body), matching {@code blockbench/camper.bbmodel} and
 * {@code blockbench/fifth_wheel_camper.bbmodel}. {@link TrailerEntity#toWorld} turns them into world positions.
 */
public final class CampLayout {
    /** A spot on the camper, in pixels. */
    public record Spot(double x, double y, double z) {
    }

    /**
     * A sleeping spot: where the sleeper's head lies (on the pillow, a little above the mattress), and which way their
     * feet point: out toward the curb side (+x), or toward the back (+z).
     */
    public record Bed(Spot head, boolean feetBack) {
    }

    private CampLayout() {
    }

    /** The camp chairs under the awning, at seat height. They face out, away from the camper. */
    public static List<Spot> chairs(TrailerType type) {
        return switch (type) {
            case CAMPER -> List.of(new Spot(36, 7.5, -22), new Spot(36, 7.5, 14));
            case FIFTH_WHEEL_CAMPER -> List.of(new Spot(40, 7.5, -2), new Spot(40, 7.5, 34));
            default -> List.of();
        };
    }

    /** Where the beds' sleepers lie. */
    public static List<Bed> beds(TrailerType type) {
        return switch (type) {
            // a double bed across the front
            case CAMPER -> List.of(new Bed(new Spot(-13, 28, -37), false), new Bed(new Spot(-13, 28, -29), false));
            // a double bed up in the bedroom over the fifth wheel, lengthways
            case FIFTH_WHEEL_CAMPER -> List.of(new Bed(new Spot(-6, 34, -64), true), new Bed(new Spot(6, 34, -64), true));
            default -> List.of();
        };
    }

    /** On the ground outside the door, past the steps: where a sleeper gets up, and their spawn point. */
    public static Spot door(TrailerType type) {
        return type == TrailerType.FIFTH_WHEEL_CAMPER ? new Spot(32, 0, -22) : new Spot(29, 0, -6);
    }

    /** The stove's vent on the roof, where its smoke comes out. */
    public static Spot vent(TrailerType type) {
        return type == TrailerType.FIFTH_WHEEL_CAMPER ? new Spot(8, 68, 18) : new Spot(-8, 52, 12);
    }
}
