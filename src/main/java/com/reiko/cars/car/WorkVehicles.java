package com.reiko.cars.car;

import com.reiko.cars.registry.ModSounds;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * What the work vehicles do besides drive. The police car and ambulance have a light bar and siren (K); the
 * ambulance heals whoever rides in the back. The dump truck tips its bed and dumps its load behind it (G). The cement
 * mixer's drum turns whenever its engine runs, making the concrete powder in it into concrete, and pours it out of
 * its chute (G). The ice cream truck's horn plays its jingle, and its serving hatch is its shop (see
 * {@code icecream/}).
 */
public final class WorkVehicles {
    /** How long the ice cream truck's jingle plays, in ticks: its horn waits that long before it can play again. */
    public static final int JINGLE_TICKS = 270;
    /** The dump truck's bed: ticks to tip it up, ticks it stays up (the load slides out partway), ticks to lower it. */
    public static final int TIP_UP = 40;
    public static final int TIP_HOLD = 30;
    public static final int TIP_DOWN = 40;
    /** When, after the bed starts up, the load slides out. */
    public static final int TIP_DUMP_AT = TIP_UP + 5;
    public static final float TIP_DEGREES = 52.0F;
    /** How often the ambulance heals its passengers, and the mixer makes a block of concrete, in ticks. */
    public static final int HEAL_EVERY = 50;
    public static final int MIX_EVERY = 10;

    private WorkVehicles() {
    }

    // ---- lights and siren ----

    /** The driver's K: lights and siren on or off. */
    public static void toggleSiren(ServerPlayer driver, CarEntity car) {
        if (!car.carType().hasSiren() || car.getControllingPassenger() != driver) {
            return;
        }
        car.setSiren(!car.sirenOn());
        driver.displayClientMessage(Component.translatable(car.sirenOn() ? "car.reikocars.siren_on" : "car.reikocars.siren_off"), true);
    }

    // ---- every tick, on the server ----

    static void serverTick(CarEntity car) {
        long now = car.level().getGameTime();
        switch (car.carType()) {
            case AMBULANCE -> {
                if (now % HEAL_EVERY == 0) {
                    heal(car);
                }
            }
            case CEMENT_MIXER -> {
                if (now % MIX_EVERY == 0 && car.isEngineRunning()) {
                    mixOne(car);
                }
            }
            case DUMP_TRUCK -> tipTick(car, now);
            case FIRE_TRUCK -> com.reiko.cars.fire.FireTrucks.serverTick(car, now);
            case ROAD_PAVER -> com.reiko.cars.road.RoadPaver.tick(car);
            default -> {
            }
        }
    }

    /** The ambulance heals everyone riding in it but the driver, half a heart at a time. */
    public static void heal(CarEntity car) {
        for (Entity passenger : car.getPassengers()) {
            if (passenger instanceof Player player && passenger != car.getControllingPassenger() && player.getHealth() < player.getMaxHealth()) {
                player.heal(1.0F);
            }
        }
    }

    /** The concrete that {@code powder} sets into, if it's concrete powder: white concrete powder to white concrete. */
    public static Item concreteFor(Item powder) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(powder);
        if (!id.getPath().endsWith("_concrete_powder")) {
            return null;
        }
        ResourceLocation set = ResourceLocation.fromNamespaceAndPath(id.getNamespace(), id.getPath().replace("_concrete_powder", "_concrete"));
        return BuiltInRegistries.ITEM.getOptional(set).orElse(null);
    }

    /** The mixer's drum turns one block of concrete powder into concrete, if it has any (and room). Returns whether it did. */
    public static boolean mixOne(CarEntity car) {
        SimpleContainer drum = car.bed();
        for (int slot = 0; slot < drum.getContainerSize(); slot++) {
            ItemStack stack = drum.getItem(slot);
            Item concrete = stack.isEmpty() ? null : concreteFor(stack.getItem());
            if (concrete == null) {
                continue;
            }
            ItemStack one = new ItemStack(concrete);
            if (stack.getCount() == 1) {
                drum.setItem(slot, ItemStack.EMPTY); // its own slot's free now, so the concrete always fits
                ItemStack left = drum.addItem(one);
                if (!left.isEmpty()) {
                    drum.setItem(slot, left);
                }
                return true;
            }
            if (!drum.canAddItem(one)) {
                continue;
            }
            stack.shrink(1);
            drum.addItem(one);
            drum.setChanged();
            return true;
        }
        return false;
    }

    // ---- the dump truck ----

    /** How far up the bed is at {@code ticks} into a tip, 0 to 1. */
    public static float tilt(float ticks) {
        if (ticks <= 0) {
            return 0.0F;
        }
        if (ticks < TIP_UP) {
            float x = ticks / TIP_UP;
            return x * x * (3 - 2 * x);
        }
        if (ticks < TIP_UP + TIP_HOLD) {
            return 1.0F;
        }
        float down = (ticks - TIP_UP - TIP_HOLD) / TIP_DOWN;
        return down >= 1.0F ? 0.0F : 1.0F - down * down * (3 - 2 * down);
    }

    /** The driver's G in the dump truck: tip the bed and dump the load. */
    public static void dump(ServerPlayer driver, CarEntity truck) {
        if (truck.tipStart() >= 0) {
            driver.displayClientMessage(Component.translatable("work.reikocars.tipping"), true);
            return;
        }
        if (truck.bed().isEmpty()) {
            driver.displayClientMessage(Component.translatable("work.reikocars.bed_empty"), true);
            return;
        }
        truck.setTipStart(truck.level().getGameTime());
        truck.level().playSound(null, truck.getX(), truck.getY(), truck.getZ(), ModSounds.HYDRAULIC.get(), SoundSource.NEUTRAL, 1.2F, 1.0F);
        driver.displayClientMessage(Component.translatable("work.reikocars.dumping"), true);
    }

    static void tipTick(CarEntity truck, long now) {
        long start = truck.tipStart();
        if (start < 0) {
            return;
        }
        long t = now - start;
        if (t == TIP_DUMP_AT) {
            spill(truck, truck.carType().length / 2.0 + 0.5);
        } else if (t == TIP_UP + TIP_HOLD) {
            truck.level().playSound(null, truck.getX(), truck.getY(), truck.getZ(), ModSounds.HYDRAULIC.get(), SoundSource.NEUTRAL, 1.0F, 0.8F);
        } else if (t >= TIP_UP + TIP_HOLD + TIP_DOWN || t < 0) {
            truck.setTipStart(-1L);
        }
    }

    /** Everything in its bed (or drum), dropped on the ground {@code back} blocks behind its middle. */
    public static int spill(CarEntity car, double back) {
        SimpleContainer load = car.bed();
        Vec3 at = car.position().add(car.forward().scale(-back)).add(0, 0.6, 0);
        Vec3 out = car.forward().scale(-0.15);
        int dropped = 0;
        for (int slot = 0; slot < load.getContainerSize(); slot++) {
            ItemStack stack = load.removeItemNoUpdate(slot);
            if (stack.isEmpty()) {
                continue;
            }
            ItemEntity item = new ItemEntity(car.level(), at.x + (car.getRandom().nextDouble() - 0.5) * 1.2, at.y,
                    at.z + (car.getRandom().nextDouble() - 0.5) * 1.2, stack, out.x, 0.1, out.z);
            item.setDefaultPickUpDelay();
            car.level().addFreshEntity(item);
            dropped += stack.getCount();
        }
        load.setChanged();
        return dropped;
    }

    // ---- the cement mixer ----

    /** The driver's G in the mixer: the drum pours everything out of the chute at the back. */
    public static void pour(ServerPlayer driver, CarEntity mixer) {
        if (mixer.bed().isEmpty()) {
            driver.displayClientMessage(Component.translatable("work.reikocars.drum_empty"), true);
            return;
        }
        int poured = spill(mixer, 68 / 16.0 + 0.4);
        mixer.level().playSound(null, mixer.getX(), mixer.getY(), mixer.getZ(), net.minecraft.sounds.SoundEvents.GRAVEL_BREAK,
                SoundSource.NEUTRAL, 1.0F, 0.8F);
        driver.displayClientMessage(Component.translatable("work.reikocars.poured", poured), true);
    }

    // ---- the ice cream truck ----

    /** Whether a click {@code at} (relative to the truck) is on its left side, where the serving hatch is. */
    public static boolean atHatch(CarEntity truck, Vec3 at) {
        float yaw = truck.getYRot() * Mth.DEG_TO_RAD;
        double left = at.x * Mth.cos(yaw) + at.z * Mth.sin(yaw);
        return left > truck.carType().width / 2.0 - 0.45;
    }
}
