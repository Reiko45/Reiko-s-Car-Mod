package com.reiko.cars.trailer;

import com.reiko.cars.car.CarEntity;
import java.util.Comparator;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundMoveVehiclePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * The hitch key (G). Driving a semi: hitches the trailer whose front you've backed up to, or drops off the one you're
 * pulling. Driving a car: drives it onto a car hauler you've pulled up behind, or back off one you're on.
 */
public final class Hitching {
    /** How close the kingpin must be to the fifth wheel, across the ground, in blocks. */
    public static final double HITCH_REACH = 1.5;
    /** How far a trailer may be turned from the tractor's heading to hitch it, in degrees. */
    public static final float HITCH_ANGLE = 45.0F;
    /** How close a car's front must be to a hauler's back end to drive on, in blocks. */
    public static final double LOAD_REACH = 2.5;

    /** What pressing the key would do now, for the dashboard's hint. */
    public enum Action {
        HITCH,
        UNHITCH,
        LOAD,
        UNLOAD
    }

    private Hitching() {
    }

    /** What the hitch key would do for {@code driver} driving {@code car}, or null if nothing. Works on either side. */
    @Nullable
    public static Action available(CarEntity car, Player driver) {
        if (car.carType().isTractor()) {
            return car.trailer() != null ? Action.UNHITCH : hitchable(car, driver) != null ? Action.HITCH : null;
        }
        if (car.getVehicle() instanceof TrailerEntity) {
            return Action.UNLOAD;
        }
        return haulerFor(car, driver) != null ? Action.LOAD : null;
    }

    /** The nearest trailer {@code tractor} could hitch: unhitched, usable, its kingpin over the fifth wheel. */
    @Nullable
    public static TrailerEntity hitchable(CarEntity tractor, Player driver) {
        Vec3 fifthWheel = tractor.fifthWheel();
        return tractor.level().getEntitiesOfClass(TrailerEntity.class, new AABB(fifthWheel, fifthWheel).inflate(8.0),
                        trailer -> !trailer.isPassenger() && trailer.isAlive() && trailer.canBeUsedBy(driver)
                                && horizontalDistance(trailer.kingpin(), fifthWheel) <= HITCH_REACH
                                && Math.abs(trailer.kingpin().y - fifthWheel.y) <= 1.0
                                && Math.abs(Mth.wrapDegrees(trailer.getYRot() - tractor.getYRot())) <= HITCH_ANGLE)
                .stream().min(Comparator.comparingDouble(trailer -> horizontalDistance(trailer.kingpin(), fifthWheel))).orElse(null);
    }

    /** The car hauler {@code car} could drive onto: one with room, whose back end its front is up against. */
    @Nullable
    public static TrailerEntity haulerFor(CarEntity car, Player driver) {
        Vec3 front = car.position().add(car.forward().scale(car.carType().length / 2.0));
        return car.level().getEntitiesOfClass(TrailerEntity.class, car.getBoundingBox().inflate(TrailerType.LENGTH),
                        hauler -> hauler.trailerType().carSlots > 0 && hauler.isAlive() && hauler.cars().size() < hauler.trailerType().carSlots
                                && hauler.canBeUsedBy(driver) && car.carType().fitsOnHauler()
                                && horizontalDistance(hauler.rear(), front) <= LOAD_REACH
                                && Math.abs(hauler.rear().y - front.y) <= 1.5
                                && Math.abs(Mth.wrapDegrees(hauler.getYRot() - car.getYRot())) <= 60.0F)
                .stream().min(Comparator.comparingDouble(hauler -> horizontalDistance(hauler.rear(), front))).orElse(null);
    }

    private static double horizontalDistance(Vec3 a, Vec3 b) {
        return Math.sqrt(Mth.square(a.x - b.x) + Mth.square(a.z - b.z));
    }

    /** The hitch key, on the server. */
    public static void use(ServerPlayer player) {
        if (!(player.getVehicle() instanceof CarEntity car) || car.getControllingPassenger() != player) {
            player.displayClientMessage(Component.translatable("trailer.reikocars.drive_first"), true);
            return;
        }
        if (car.carType().isTractor()) {
            TrailerEntity hitched = car.trailer();
            if (hitched != null) {
                hitched.stopRiding();
                clank(car, 0.8F);
                player.displayClientMessage(Component.translatable("trailer.reikocars.unhitched", hitched.getDisplayName()), true);
                return;
            }
            TrailerEntity trailer = hitchable(car, player);
            if (trailer == null) {
                player.displayClientMessage(Component.translatable("trailer.reikocars.nothing_to_hitch"), true);
                return;
            }
            trailer.startRiding(car, true);
            clank(car, 1.1F);
            player.displayClientMessage(Component.translatable("trailer.reikocars.hitched", trailer.getDisplayName()), true);
            return;
        }
        if (car.getVehicle() instanceof TrailerEntity hauler) {
            unload(player, car, hauler);
            return;
        }
        TrailerEntity hauler = haulerFor(car, player);
        if (hauler == null) {
            player.displayClientMessage(Component.translatable("trailer.reikocars.no_hauler"), true);
            return;
        }
        car.stopDead();
        car.startRiding(hauler, true);
        clank(hauler, 1.3F);
        player.displayClientMessage(Component.translatable("trailer.reikocars.loaded", hauler.cars().size(), hauler.trailerType().carSlots), true);
    }

    /** Rolls {@code car} off the back of {@code hauler}, turned to drive away. */
    private static void unload(ServerPlayer player, CarEntity car, TrailerEntity hauler) {
        Vec3 back = hauler.forward().scale(-1.0);
        Vec3 spot = hauler.rear().add(back.scale(car.carType().length / 2.0 + 0.75));
        AABB room = car.getType().getDimensions().makeBoundingBox(spot);
        if (!car.level().noBlockCollision(car, room)) {
            player.displayClientMessage(Component.translatable("trailer.reikocars.no_room_behind"), true);
            return;
        }
        car.stopRiding();
        car.stopDead();
        car.moveTo(spot.x, spot.y, spot.z, hauler.getYRot() + 180.0F, 0.0F);
        // The driver's game moves the car it drives, so tell it where the car is now.
        player.connection.send(new ClientboundMoveVehiclePacket(car));
        clank(hauler, 1.0F);
        player.displayClientMessage(Component.translatable("trailer.reikocars.unloaded"), true);
    }

    private static void clank(net.minecraft.world.entity.Entity at, float pitch) {
        at.level().playSound(null, at.getX(), at.getY(), at.getZ(), SoundEvents.CHAIN_PLACE, SoundSource.NEUTRAL, 1.0F, pitch);
        at.level().playSound(null, at.getX(), at.getY(), at.getZ(), SoundEvents.IRON_DOOR_CLOSE, SoundSource.NEUTRAL, 0.5F, pitch * 0.8F);
    }
}
