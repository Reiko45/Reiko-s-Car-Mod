package com.reiko.cars.trailer;

import com.reiko.cars.car.CarEntity;
import com.reiko.cars.mail.MailService;
import com.reiko.cars.toll.TollService;
import com.reiko.cars.tow.Towing;
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
 * pulling; a pickup or SUV does the same with the bumper-pull trailers (campers and livestock trailers) on its tow
 * ball. Driving a tow truck: picks up the car behind, or puts it down ({@link Towing}). Driving the mail van:
 * collects and delivers the mail at the mailboxes beside it ({@link MailService}). Driving a car: drives it onto a car
 * hauler you've pulled up behind, or back off one you're on.
 */
public final class Hitching {
    /** How close the kingpin must be to the fifth wheel (or the coupler to the ball), across the ground, in blocks. */
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
        UNLOAD,
        TOW,
        DROP,
        MAIL,
        TOLL,
        WASH,
        TRASH,
        DUMP,
        POUR,
        LADDER_UP,
        LADDER_DOWN,
        DOORS,
        GARAGE,
        PARK,
        LEAVE_PARKING,
        PAVE_START,
        PAVE_STOP
    }

    private Hitching() {
    }

    /** What the hitch key would do for {@code driver} driving {@code car}, or null if nothing. Works on either side. */
    @Nullable
    public static Action available(CarEntity car, Player driver) {
        if (TollService.gateAhead(car) != null) {
            return Action.TOLL;
        }
        if (car.dirt() > 0.0F && com.reiko.cars.wash.CarWashService.washFor(car) != null) {
            return Action.WASH;
        }
        com.reiko.cars.garage.GarageDoorBlockEntity garage = com.reiko.cars.garage.ParkingService.doorAhead(car);
        if (garage != null) {
            if (com.reiko.cars.garage.ParkingService.wantsTicket(car, driver, garage)) {
                return com.reiko.cars.garage.GarageDoors.depth(car.position(), garage) < 0.0 ? Action.LEAVE_PARKING : Action.PARK;
            }
            if (com.reiko.cars.garage.ParkingService.through(car, driver, garage)
                    || (garage.settings().parking() && com.reiko.cars.garage.ParkingService.paidUp(car, garage))) {
                return Action.GARAGE;
            }
        }
        if (car.carType().runsRoutes()) {
            return com.reiko.cars.bus.BusService.stopNear(car) != null ? Action.DOORS : null;
        }
        if (car.carType().isTowTruck()) {
            return car.towed() != null ? Action.DROP : Towing.towable(car, driver) != null ? Action.TOW : null;
        }
        if (car.carType().isMailVan() && MailService.mailboxesNear(car).size() > 0) {
            return Action.MAIL;
        }
        switch (car.carType()) {
            case GARBAGE_TRUCK -> {
                return com.reiko.cars.garbage.GarbageService.cansNear(car).isEmpty() ? null : Action.TRASH;
            }
            case DUMP_TRUCK -> {
                return car.tipStart() >= 0 ? null : Action.DUMP;
            }
            case CEMENT_MIXER -> {
                return Action.POUR;
            }
            case FIRE_TRUCK -> {
                return car.ladderUp() ? Action.LADDER_DOWN : Action.LADDER_UP;
            }
            case ROAD_PAVER -> {
                return car.paving() ? Action.PAVE_STOP : Action.PAVE_START;
            }
            default -> {
            }
        }
        if (car.carType().isTractor()) {
            return car.trailer() != null ? Action.UNHITCH : hitchable(car, driver) != null ? Action.HITCH : null;
        }
        if (car.getVehicle() instanceof TrailerEntity) {
            return Action.UNLOAD;
        }
        if (car.carType().hasTowBall()) {
            if (car.trailer() != null) {
                return Action.UNHITCH;
            }
            if (hitchable(car, driver) != null) {
                return Action.HITCH;
            }
        }
        return haulerFor(car, driver) != null ? Action.LOAD : null;
    }

    /**
     * The nearest trailer {@code tractor} could hitch: unhitched, usable, of a kind it can pull, its kingpin over the
     * fifth wheel (or its coupler over the tow ball).
     */
    @Nullable
    public static TrailerEntity hitchable(CarEntity tractor, Player driver) {
        Vec3 hitch = tractor.hitchPoint();
        return tractor.level().getEntitiesOfClass(TrailerEntity.class, new AABB(hitch, hitch).inflate(8.0),
                        trailer -> !trailer.isPassenger() && trailer.isAlive() && trailer.canBeUsedBy(driver)
                                && tractor.carType().canTow(trailer.trailerType())
                                && horizontalDistance(trailer.coupler(), hitch) <= HITCH_REACH
                                && Math.abs(trailer.coupler().y - hitch.y) <= 1.0
                                && Math.abs(Mth.wrapDegrees(trailer.getYRot() - tractor.getYRot())) <= HITCH_ANGLE)
                .stream().min(Comparator.comparingDouble(trailer -> horizontalDistance(trailer.coupler(), hitch))).orElse(null);
    }

    /** The car hauler {@code car} could drive onto: one with room, whose back end its front is up against. */
    @Nullable
    public static TrailerEntity haulerFor(CarEntity car, Player driver) {
        Vec3 front = car.position().add(car.forward().scale(car.carType().length / 2.0));
        return car.level().getEntitiesOfClass(TrailerEntity.class, car.getBoundingBox().inflate(TrailerType.MAX_LENGTH),
                        hauler -> hauler.trailerType().carSlots > 0 && hauler.isAlive() && hauler.cars().size() < hauler.trailerType().carSlots
                                && hauler.canBeUsedBy(driver) && car.carType().fitsOnHauler() && car.trailer() == null
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
        if (player.getVehicle() instanceof CarEntity bus && bus.carType().runsRoutes() && bus.getControllingPassenger() != player) {
            com.reiko.cars.bus.BusService.ringBell(player, bus); // a passenger rings for the next stop
            return;
        }
        if (!(player.getVehicle() instanceof CarEntity car) || car.getControllingPassenger() != player) {
            player.displayClientMessage(Component.translatable("trailer.reikocars.drive_first"), true);
            return;
        }
        if (TollService.use(player, car)) {
            return;
        }
        if (com.reiko.cars.wash.CarWashService.use(player, car)) {
            return;
        }
        if (com.reiko.cars.garage.ParkingService.use(player, car)) {
            return;
        }
        if (car.carType().runsRoutes()) {
            if (!com.reiko.cars.bus.BusService.openDoors(player, car)) {
                player.displayClientMessage(Component.translatable("bus.reikocars.no_stop"), true);
            }
            return;
        }
        if (car.carType().isTowTruck()) {
            Towing.use(player, car);
            return;
        }
        if (car.carType().isMailVan() && !MailService.mailboxesNear(car).isEmpty()) {
            MailService.serve(player, car);
            return;
        }
        switch (car.carType()) {
            case GARBAGE_TRUCK -> {
                com.reiko.cars.garbage.GarbageService.collect(player, car);
                return;
            }
            case DUMP_TRUCK -> {
                com.reiko.cars.car.WorkVehicles.dump(player, car);
                return;
            }
            case CEMENT_MIXER -> {
                com.reiko.cars.car.WorkVehicles.pour(player, car);
                return;
            }
            case FIRE_TRUCK -> {
                com.reiko.cars.fire.FireLadder.toggle(player, car);
                return;
            }
            case ROAD_PAVER -> {
                com.reiko.cars.road.RoadPaver.toggle(player, car);
                return;
            }
            default -> {
            }
        }
        if (car.carType().isTractor() || (car.carType().hasTowBall() && !(car.getVehicle() instanceof TrailerEntity)
                && (car.trailer() != null || hitchable(car, player) != null))) {
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
            if (trailer.isBrokenDown()) {
                // A trailer with a part worn out can't be towed: it has to be fixed where it stands.
                player.displayClientMessage(Component.translatable("trailer.reikocars.too_worn_to_hitch", trailer.getDisplayName(),
                        trailer.condition().brokenPart().displayName()), true);
                return;
            }
            trailer.startRiding(car, true);
            clank(car, 1.1F);
            player.displayClientMessage(Component.translatable("trailer.reikocars.hitched", trailer.getDisplayName()), true);
            com.reiko.cars.advancement.CarAdvancements.award(player, "hitch");
            if (car.carType().isTractor() && trailer.trailerType().isSemiTrailer()) {
                com.reiko.cars.advancement.CarAdvancements.award(player, "semi");
            }
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
