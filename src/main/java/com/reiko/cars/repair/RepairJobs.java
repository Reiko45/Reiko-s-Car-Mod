package com.reiko.cars.repair;

import com.reiko.cars.car.CarCondition;
import com.reiko.cars.car.CarCondition.Part;
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
 *   <li>Brakes: a set of Brake Pads (two for a diesel truck or a trailer), as new.</li>
 *   <li>Body: Body Panels, each mending a fifth of the body (a tenth of a truck's or a trailer's).</li>
 * </ul>
 *
 * An oil change, tires and brakes are all-or-nothing; engine and body repairs can be done a piece at a time. A
 * trailer has no oil or engine to fix.
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

    /** Whether the job needs every item at once (an oil change, a set of tires or brakes). */
    public static boolean allAtOnce(Part part) {
        return part == Part.OIL || part == Part.TIRES || part == Part.BRAKES;
    }

    /** How much of a part one item puts back, for the jobs done a piece at a time. */
    private static float perItem(Part part, Repairable vehicle) {
        float base = part == Part.ENGINE ? 0.25F : 0.2F;
        return vehicle.isHeavy() ? base / 2.0F : base;
    }

    /** How many of the part's items it takes to make {@code vehicle}'s part as good as new: 0 if it's fine (or missing). */
    public static int needed(Part part, Repairable vehicle) {
        return needed(part, vehicle, vehicle.condition());
    }

    /** How many of the part's items it takes to make the part as good as new from {@code condition}. */
    public static int needed(Part part, Repairable vehicle, CarCondition condition) {
        float value = condition.get(part);
        if (value >= 0.995F || !vehicle.hasPart(part)) {
            return 0;
        }
        return switch (part) {
            case OIL, BRAKES -> vehicle.isHeavy() ? 2 : 1;
            case TIRES -> vehicle.wheelCount();
            case ENGINE, BODY -> (int) Math.ceil((1.0F - value) / perItem(part, vehicle) - 1.0E-4);
        };
    }

    /** The condition after fitting {@code count} of the part's items (a whole set, for the all-at-once jobs). */
    public static CarCondition fit(Part part, Repairable vehicle, CarCondition condition, int count) {
        if (count <= 0 || !vehicle.hasPart(part)) {
            return condition;
        }
        if (allAtOnce(part)) {
            return count >= needed(part, vehicle, condition) ? condition.with(part, 1.0F) : condition;
        }
        return condition.with(part, Math.min(1.0F, condition.get(part) + count * perItem(part, vehicle)));
    }
}
