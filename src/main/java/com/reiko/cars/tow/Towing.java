package com.reiko.cars.tow;

import com.reiko.cars.car.CarEntity;
import com.reiko.cars.car.CarType;
import java.util.Comparator;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * The tow trucks. Driving one, the hitch key (G) picks up the car behind: the flatbed winches it up onto its deck,
 * facing forward; the wrecker lifts its front wheels on the wheel-lift and tows it with its back wheels on the road,
 * swinging round behind on bends as a trailer does. G again puts it down. A towed car rides the truck as a passenger,
 * so it goes wherever the truck goes, and is saved with it. It doesn't drive, burn fuel or wear while it's towed.
 *
 * <p>Any car the driver may use can be towed (their own, or an unlocked one), broken down or not, as long as nobody is
 * in it and it isn't too big (see {@link CarType#towableBy}).
 */
public final class Towing {
    /** How close, in blocks, the car must be to the deck's tail or the wheel-lift to be picked up. */
    public static final double REACH = 2.5;
    /** The flatbed's deck: its top's height and the back of its tail, and where the head of it is. */
    public static final double DECK_TOP = 20 / 16.0;
    public static final double DECK_TAIL = 59 / 16.0;
    private static final double DECK_HEAD = -2 / 16.0;
    private static final double DECK_MIDDLE = 27 / 16.0;
    /** The wrecker's wheel-lift: how far behind the truck's middle the forks hold a car's front wheels. */
    public static final double LIFT_BACK = 61 / 16.0;
    /** How far the wheel-lift raises a car's front wheels. */
    public static final double LIFT_RISE = 0.35;
    /** How far a car on the wheel-lift can swing round from straight behind, in degrees. */
    public static final float MAX_SWING = 70.0F;
    /** How much slower a loaded tow truck is: top speed and acceleration. */
    public static final double LOADED_SPEED = 0.9;
    public static final double LOADED_ACCELERATION = 0.8;

    private Towing() {
    }

    /** The car {@code truck} is towing, if any. */
    @Nullable
    public static CarEntity towed(CarEntity truck) {
        for (var passenger : truck.getPassengers()) {
            if (passenger instanceof CarEntity car) {
                return car;
            }
        }
        return null;
    }

    /** How far ahead of its middle a car's front axle is, roughly, in blocks. */
    public static double frontAxle(CarType type) {
        return type.length * 0.32;
    }

    /** Where the flatbed's tail or the wrecker's forks are, on the ground. */
    public static Vec3 pickupPoint(CarEntity truck) {
        double back = truck.carType() == CarType.FLATBED ? DECK_TAIL : LIFT_BACK;
        return truck.position().add(truck.forward().scale(-back));
    }

    /**
     * The car {@code truck} would pick up: the nearest one {@code driver} may use, with nobody in it, small enough,
     * whose front (for the wrecker) or nearer end (for the flatbed) is up against the truck's back.
     */
    @Nullable
    public static CarEntity towable(CarEntity truck, Player driver) {
        CarType type = truck.carType();
        if (!type.isTowTruck() || towed(truck) != null) {
            return null;
        }
        Vec3 point = pickupPoint(truck);
        return truck.level().getEntitiesOfClass(CarEntity.class, new AABB(point, point).inflate(8.0),
                        car -> car != truck && car.isAlive() && !car.isPassenger() && car.getPassengers().isEmpty()
                                && car.carType().towableBy(type) && car.canBeUsedBy(driver)
                                && Math.abs(car.getY() - truck.getY()) <= 1.5
                                && reach(truck, car, point) <= REACH)
                .stream().min(Comparator.comparingDouble(car -> reach(truck, car, point))).orElse(null);
    }

    /** How far the car's end is from the pickup point: its front for the wrecker, either end for the flatbed. */
    private static double reach(CarEntity truck, CarEntity car, Vec3 point) {
        Vec3 half = car.forward().scale(car.carType().length / 2.0);
        double front = horizontal(car.position().add(half), point);
        if (truck.carType() == CarType.WRECKER) {
            // facing the same way as the truck, more or less, so its front is toward the truck's back
            float off = Math.abs(Mth.wrapDegrees(car.getYRot() - truck.getYRot()));
            return off <= 60.0F ? front : Double.MAX_VALUE;
        }
        return Math.min(front, horizontal(car.position().subtract(half), point));
    }

    private static double horizontal(Vec3 a, Vec3 b) {
        return Math.sqrt(Mth.square(a.x - b.x) + Mth.square(a.z - b.z));
    }

    /** The hitch key in a tow truck, on the server: picks up the car behind, or puts down the one it has. */
    public static void use(ServerPlayer driver, CarEntity truck) {
        CarEntity carried = towed(truck);
        if (carried != null) {
            drop(driver, truck, carried);
            return;
        }
        CarEntity car = towable(truck, driver);
        if (car == null) {
            driver.displayClientMessage(Component.translatable(truck.carType() == CarType.FLATBED
                    ? "tow.reikocars.nothing_flatbed" : "tow.reikocars.nothing_wrecker"), true);
            return;
        }
        car.stopDead();
        if (!car.startRiding(truck, true)) {
            driver.displayClientMessage(Component.translatable("tow.reikocars.cant_tow", car.getDisplayName()), true);
            return;
        }
        sound(truck, true);
        driver.displayClientMessage(Component.translatable("tow.reikocars.towing", car.getDisplayName()), true);
    }

    /** Puts the towed car down: rolled off the back of the flatbed, or let down off the wheel-lift where it is. */
    public static boolean drop(@Nullable ServerPlayer driver, CarEntity truck, CarEntity car) {
        Vec3 spot;
        float yaw;
        if (truck.carType() == CarType.FLATBED) {
            Vec3 back = truck.forward().scale(-1.0);
            spot = pickupPoint(truck).add(back.scale(car.carType().length / 2.0 + 0.5));
            yaw = truck.getYRot();
        } else {
            spot = new Vec3(car.getX(), truck.getY(), car.getZ());
            yaw = car.getYRot();
        }
        spot = new Vec3(spot.x, truck.getY(), spot.z);
        AABB room = car.getType().getDimensions().makeBoundingBox(spot);
        if (!car.level().noBlockCollision(car, room)) {
            if (driver != null) {
                driver.displayClientMessage(Component.translatable("tow.reikocars.no_room", car.getDisplayName()), true);
            }
            return false;
        }
        car.stopRiding();
        car.stopDead();
        car.setTowPitch(0.0F);
        car.moveTo(spot.x, spot.y, spot.z, yaw, 0.0F);
        sound(truck, false);
        if (driver != null) {
            driver.displayClientMessage(Component.translatable("tow.reikocars.dropped", car.getDisplayName()), true);
        }
        return true;
    }

    private static void sound(CarEntity truck, boolean up) {
        Vec3 at = pickupPoint(truck);
        truck.level().playSound(null, at.x, at.y, at.z, SoundEvents.CHAIN_PLACE, SoundSource.NEUTRAL, 1.0F, up ? 0.8F : 1.0F);
        truck.level().playSound(null, at.x, at.y, at.z, up ? SoundEvents.PISTON_EXTEND : SoundEvents.PISTON_CONTRACT,
                SoundSource.NEUTRAL, 0.6F, 0.7F);
    }

    /**
     * Puts the towed car where the truck holds it, every tick, on both sides. On the flatbed: on the deck, facing
     * forward, its middle a little behind the deck's (further back for a long one, so it clears the winch). On the
     * wrecker: its front wheels in the forks, raised, its back wheels trailing behind on the road.
     */
    public static void position(CarEntity truck, CarEntity car, Entity.MoveFunction move) {
        if (truck.carType() == CarType.FLATBED) {
            double middle = Math.max(DECK_MIDDLE, -DECK_HEAD + car.carType().length / 2.0);
            Vec3 at = truck.position().add(truck.forward().scale(-middle));
            move.accept(car, at.x, truck.getY() + DECK_TOP, at.z);
            car.setYRot(truck.getYRot());
            car.setTowPitch(0.0F);
            return;
        }
        Vec3 lift = pickupPoint(truck);
        double axle = frontAxle(car.carType());
        // The back axle trails: the car points from where its back axle was toward the lift.
        Vec3 rear = car.position().subtract(car.forward().scale(axle));
        double dx = lift.x - rear.x;
        double dz = lift.z - rear.z;
        float yaw = dx * dx + dz * dz < 1.0E-6 ? car.getYRot() : (float) (Mth.atan2(-dx, dz) * Mth.RAD_TO_DEG);
        float relative = Mth.wrapDegrees(yaw - truck.getYRot());
        if (Math.abs(relative) > MAX_SWING) {
            yaw = truck.getYRot() + Math.copySign(MAX_SWING, relative);
        }
        yaw = car.getYRot() + Mth.wrapDegrees(yaw - car.getYRot()); // turn smoothly, never jumping by 360
        float radians = yaw * Mth.DEG_TO_RAD;
        Vec3 direction = new Vec3(-Mth.sin(radians), 0.0, Mth.cos(radians));
        move.accept(car, lift.x - direction.x * axle, truck.getY() + LIFT_RISE / 2.0, lift.z - direction.z * axle);
        car.setYRot(yaw);
        car.setTowPitch((float) Math.toDegrees(Math.asin(Math.min(1.0, LIFT_RISE / (2.0 * axle)))));
    }
}
