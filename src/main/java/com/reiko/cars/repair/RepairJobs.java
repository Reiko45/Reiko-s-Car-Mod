package com.reiko.cars.repair;

import com.reiko.cars.car.CarCondition;
import com.reiko.cars.car.CarCondition.Part;
import com.reiko.cars.car.CarType;
import com.reiko.cars.car.FuelType;
import com.reiko.cars.registry.ModItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * What it takes to fix each part, and what fitting it does:
 *
 * <ul>
 *   <li>Oil: an oil change, a bottle of Motor Oil (two for a diesel truck), back to full.</li>
 *   <li>Engine: Engine Parts, each putting back a quarter of the engine (an eighth of a truck's).</li>
 *   <li>Tires: a new set, one Tire per wheel.</li>
 *   <li>Brakes: a set of Brake Pads (two for a diesel truck), as new.</li>
 *   <li>Body: Body Panels, each mending a fifth of the body (a tenth of a truck's).</li>
 * </ul>
 *
 * An oil change, tires and brakes are all-or-nothing; engine and body repairs can be done a piece at a time.
 */
public final class RepairJobs {
    private RepairJobs() {
    }

    /** The item that fixes {@code part}. */
    public static Item item(Part part) {
        return switch (part) {
            case OIL -> ModItems.MOTOR_OIL.get();
            case ENGINE -> ModItems.ENGINE_PARTS.get();
            case TIRES -> ModItems.TIRE.get();
            case BRAKES -> ModItems.BRAKE_PADS.get();
            case BODY -> ModItems.BODY_PANEL.get();
        };
    }

    /** The part {@code stack} fixes, if any. */
    @Nullable
    public static Part partFor(ItemStack stack) {
        for (Part part : Part.values()) {
            if (stack.is(item(part))) {
                return part;
            }
        }
        return null;
    }

    /** Diesel trucks take twice the oil, brake pads, engine parts and body panels. */
    public static boolean isHeavy(CarType type) {
        return type.fuel() == FuelType.DIESEL;
    }

    /** Whether the job needs every item at once (an oil change, a set of tires or brakes). */
    public static boolean allAtOnce(Part part) {
        return part == Part.OIL || part == Part.TIRES || part == Part.BRAKES;
    }

    /** How much of a part one item puts back, for the jobs done a piece at a time. */
    private static float perItem(Part part, CarType type) {
        float base = part == Part.ENGINE ? 0.25F : 0.2F;
        return isHeavy(type) ? base / 2.0F : base;
    }

    /** How many of the part's items it takes to make it as good as new: 0 if it's fine as it is. */
    public static int needed(Part part, CarType type, CarCondition condition) {
        float value = condition.get(part);
        if (value >= 0.995F) {
            return 0;
        }
        return switch (part) {
            case OIL, BRAKES -> isHeavy(type) ? 2 : 1;
            case TIRES -> type.wheels;
            case ENGINE, BODY -> (int) Math.ceil((1.0F - value) / perItem(part, type) - 1.0E-4);
        };
    }

    /** The condition after fitting {@code count} of the part's items (a whole set, for the all-at-once jobs). */
    public static CarCondition fit(Part part, CarType type, CarCondition condition, int count) {
        if (count <= 0) {
            return condition;
        }
        if (allAtOnce(part)) {
            return count >= needed(part, type, condition) ? condition.with(part, 1.0F) : condition;
        }
        return condition.with(part, Math.min(1.0F, condition.get(part) + count * perItem(part, type)));
    }
}
