package com.reiko.cars.bus;

import com.reiko.cars.ReikoCars;
import com.reiko.cars.car.CarEntity;
import com.reiko.cars.config.CarsConfig;
import com.reiko.cars.job.CarJobs;
import com.reiko.cars.registry.ModSounds;
import com.reiko.economy.api.EconomyApi;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.jetbrains.annotations.Nullable;

/**
 * Bus stops at work. Players wait at a stop (right-clicking its sign, or sitting on its shelter's bench); a bus driver
 * who's stopped by a stop presses the hitch key (G) to open the doors, and then:
 * <ol>
 *   <li>passengers who rang for the stop (G, as a passenger) get off, and villagers whose stop it is get off and pay the
 *   driver;</li>
 *   <li>players waiting there get on (and pay their fare, as anyone getting on a bus does), and villagers by the stop
 *   may get on, if it's on the bus's route;</li>
 *   <li>if the stop's on the bus's route, the bus heads for the next one, everyone aboard is told it, and the driver's
 *   paid by the job board for a stop served in order.</li>
 * </ol>
 */
@EventBusSubscriber(modid = ReikoCars.MODID)
public final class BusService {
    /** How far from a stop someone waiting there can stand. */
    static final double WAIT_REACH = 6.0;
    /** How long someone waits at a stop after right-clicking it, in ticks (5 minutes). */
    static final long WAIT_TICKS = 6000;
    /** The shortest way between two stops for the second to pay the driver, in blocks (and the bus must drive most of it). */
    public static final double MIN_LEG = 24.0;
    /** How close to a stop a villager has to be to get on there. */
    static final double VILLAGER_REACH = 5.0;
    /** The most villagers that get on at one stop. */
    static final int VILLAGERS_A_STOP = 3;
    /** How far a villager goes at least, in blocks. */
    public static final double MIN_VILLAGER_TRIP = 32.0;
    /** How long after a ride before a villager rides again, in ticks (half a Minecraft day). */
    static final long VILLAGER_REST = 12000;
    /** A villager who's ridden this long without getting to their stop gets off at the next one, in ticks. */
    static final long VILLAGER_MAX_RIDE = 12000;
    private static final String RODE = "reikocars_bus_ride";

    /** Who's waiting at which stop, and until when. */
    private record Waiting(GlobalPos stop, long until) {
    }

    private static final Map<UUID, Waiting> waiting = new HashMap<>();

    private BusService() {
    }

    // ---- at the stop ----

    /** Right-clicking a stop: its owner gets their screen (unless sneaking); anyone else waits there. */
    public static void useStop(ServerPlayer player, BusStopBlockEntity stop, boolean seated) {
        if (stop.canManage(player) && stop.owner() != null && !player.isSecondaryUseActive() && !seated) {
            BusStopMenu.open(player, stop);
            return;
        }
        waitAt(player, stop, seated);
    }

    /** Sits {@code player} on the bench in the shelter block at {@code pos}, if there's room. Returns whether they sat. */
    public static boolean sit(ServerPlayer player, BusStopBlockEntity stop, BlockState state, BlockPos pos) {
        if (player.isPassenger()) {
            return false;
        }
        Level level = player.level();
        if (!level.getEntitiesOfClass(BenchSeatEntity.class, new AABB(pos)).isEmpty()) {
            return false; // someone's sitting there
        }
        BenchSeatEntity seat = BenchSeatEntity.at(level, state, pos);
        level.addFreshEntity(seat);
        if (!player.startRiding(seat, true)) {
            seat.discard();
            return false;
        }
        waitAt(player, stop, true);
        return true;
    }

    private static void waitAt(ServerPlayer player, BusStopBlockEntity stop, boolean seated) {
        GlobalPos at = GlobalPos.of(player.level().dimension(), stop.getBlockPos());
        waiting.put(player.getUUID(), new Waiting(at, player.level().getGameTime() + WAIT_TICKS));
        Component name = stop.displayName();
        Component line = stop.routes().isEmpty() ? Component.translatable("bus.reikocars.waiting_no_routes", name)
                : Component.translatable(seated ? "bus.reikocars.sitting" : "bus.reikocars.waiting", name, stop.routesText());
        player.displayClientMessage(line, true);
    }

    /** Whether {@code player} is waiting at {@code stop}: sat on its bench, or told it they are and still close by. */
    public static boolean waitingAt(ServerPlayer player, BusStopBlockEntity stop) {
        if (player.getVehicle() instanceof BenchSeatEntity seat) {
            BlockState state = player.level().getBlockState(seat.blockPosition());
            return state.getBlock() instanceof BusShelterBlock && BusShelterBlock.mainPos(state, seat.blockPosition()).equals(stop.getBlockPos());
        }
        Waiting wait = waiting.get(player.getUUID());
        return wait != null && !player.isPassenger() && wait.stop().equals(GlobalPos.of(player.level().dimension(), stop.getBlockPos()))
                && player.level().getGameTime() <= wait.until() && player.position().distanceTo(spot(stop)) <= WAIT_REACH;
    }

    @SubscribeEvent
    static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        waiting.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    static void onServerStopped(net.neoforged.neoforge.event.server.ServerStoppedEvent event) {
        waiting.clear();
    }

    /** Where a stop is, for distances: the sign's block, or the middle of the shelter. */
    public static Vec3 spot(BusStopBlockEntity stop) {
        BlockState state = stop.getBlockState();
        if (state.getBlock() instanceof BusShelterBlock) {
            return Vec3.atBottomCenterOf(BusShelterBlock.partPos(state, stop.getBlockPos(), 1, 0));
        }
        return Vec3.atBottomCenterOf(stop.getBlockPos());
    }

    /** The stop a bus is at: the nearest within {@code busStops.stopReach} of it. Works on either side. */
    @Nullable
    public static BusStopBlockEntity stopNear(CarEntity bus) {
        if (!bus.carType().runsRoutes()) {
            return null;
        }
        double reach = CarsConfig.get(CarsConfig.STOP_REACH);
        AABB near = bus.getBoundingBox().inflate(reach, 3.0, reach);
        Level level = bus.level();
        BusStopBlockEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (int cx = (int) Math.floor(near.minX) >> 4; cx <= (int) Math.floor(near.maxX) >> 4; cx++) {
            for (int cz = (int) Math.floor(near.minZ) >> 4; cz <= (int) Math.floor(near.maxZ) >> 4; cz++) {
                if (!level.hasChunk(cx, cz)) {
                    continue;
                }
                for (BlockEntity entity : level.getChunk(cx, cz).getBlockEntities().values()) {
                    if (entity instanceof BusStopBlockEntity stop && !stop.isRemoved()) {
                        Vec3 at = spot(stop);
                        if (near.contains(at.add(0.0, 0.5, 0.0))) {
                            double distance = at.distanceToSqr(bus.position());
                            if (distance < bestDistance) {
                                bestDistance = distance;
                                best = stop;
                            }
                        }
                    }
                }
            }
        }
        return best;
    }

    // ---- the bell ----

    /** A passenger rings for the next stop. */
    public static void ringBell(ServerPlayer rider, CarEntity bus) {
        if (!bus.busLine().ring(rider.getUUID())) {
            rider.displayClientMessage(Component.translatable("bus.reikocars.already_rung"), true);
            return;
        }
        bus.level().playSound(null, bus.getX(), bus.getY() + 1.5, bus.getZ(), SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.NEUTRAL, 0.8F, 1.6F);
        rider.displayClientMessage(Component.translatable("bus.reikocars.rung"), true);
        if (bus.getControllingPassenger() instanceof ServerPlayer driver && driver != rider) {
            driver.displayClientMessage(Component.translatable("bus.reikocars.stop_requested"), true);
        }
    }

    // ---- the doors ----

    /**
     * The driver's hitch key: if the bus is at a stop, opens the doors there (and returns true); if it's still moving,
     * says so.
     */
    public static boolean openDoors(ServerPlayer driver, CarEntity bus) {
        BusStopBlockEntity stop = stopNear(bus);
        if (stop == null || !(bus.level() instanceof ServerLevel level)) {
            return false;
        }
        if (!bus.standingStill()) {
            driver.displayClientMessage(Component.translatable("bus.reikocars.stop_first"), true);
            return true;
        }
        BusLine line = bus.busLine();
        GlobalPos here = GlobalPos.of(level.dimension(), stop.getBlockPos());
        long now = level.getGameTime();
        level.playSound(null, bus.getX(), bus.getY() + 1.0, bus.getZ(), ModSounds.HYDRAULIC.get(), SoundSource.NEUTRAL, 0.8F, 1.6F);
        int off = letOff(level, bus, here, now, driver);
        int on = letOn(level, bus, stop);
        BusStopRegistry.Stop onRoute = null;
        List<BusStopRegistry.Stop> route = line.stops();
        for (BusStopRegistry.Stop each : route) {
            if (each.pos().equals(here)) {
                onRoute = each;
            }
        }
        if (onRoute != null) {
            on += villagersOn(level, bus, stop, here, route, now);
            serve(driver, bus, onRoute, here);
        }
        line.clearBell();
        // Tell everyone aboard (and the driver) where they are and where next.
        BusStopRegistry.Stop next = onRoute != null ? line.nextStop() : null;
        Component atStop = stop.displayName();
        Component message = next != null && !next.pos().equals(here)
                ? Component.translatable("bus.reikocars.this_stop_next", atStop, next.name().isEmpty()
                        ? Component.translatable("bus.reikocars.unnamed_stop") : Component.literal(next.name()))
                : Component.translatable("bus.reikocars.this_stop", atStop);
        for (Entity passenger : bus.getPassengers()) {
            if (passenger instanceof ServerPlayer rider) {
                rider.displayClientMessage(message, true);
            }
        }
        level.playSound(null, bus.getX(), bus.getY() + 1.5, bus.getZ(), SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.NEUTRAL, 0.7F, 1.2F);
        if (off + on > 0) {
            ReikoCars.LOGGER.debug("Bus at {}: {} off, {} on", stop.getBlockPos(), off, on);
        }
        return true;
    }

    /** Passengers who rang get off, and villagers whose stop it is (or who've ridden too long). Returns how many. */
    private static int letOff(ServerLevel level, CarEntity bus, GlobalPos here, long now, ServerPlayer driver) {
        BusLine line = bus.busLine();
        int off = 0;
        for (Entity passenger : List.copyOf(bus.getPassengers())) {
            if (passenger == driver) {
                continue;
            }
            if (passenger instanceof ServerPlayer rider && line.rung(rider.getUUID())) {
                rider.stopRiding();
                rider.displayClientMessage(Component.translatable("bus.reikocars.got_off", stopName(level, here)), true);
                off++;
            } else if (passenger instanceof Villager villager) {
                BusLine.Ride ride = line.villagers().get(villager.getUUID());
                boolean theirs = ride != null && ride.to().equals(here);
                if (ride == null || theirs || now - ride.boardedAt() > VILLAGER_MAX_RIDE) {
                    villager.stopRiding();
                    villager.getPersistentData().putLong(RODE, now);
                    off++;
                    if (theirs && ride.from().dimension() == here.dimension()
                            && Math.sqrt(ride.from().pos().distSqr(here.pos())) >= MIN_VILLAGER_TRIP) {
                        villagerPays(level, driver, villager);
                    }
                }
            }
        }
        return off;
    }

    private static void villagerPays(ServerLevel level, ServerPlayer driver, Villager villager) {
        long fare = Math.round(CarsConfig.get(CarsConfig.VILLAGER_FARE) * 100.0);
        if (fare > 0) {
            EconomyApi.get().deposit(level.getServer(), driver.getUUID(), fare, "reikocars.villager_fare");
            driver.sendSystemMessage(Component.translatable("bus.reikocars.villager_paid", villager.getDisplayName(), EconomyApi.get().format(fare)));
        }
        CarJobs.work(driver, CarJobs.BUS_RIDER, 1);
    }

    /** Players waiting at the stop get on, while there are seats. Returns how many. */
    private static int letOn(ServerLevel level, CarEntity bus, BusStopBlockEntity stop) {
        int on = 0;
        Vec3 at = spot(stop);
        // Those who said they're waiting, and those sat on the shelter's bench.
        java.util.Set<ServerPlayer> candidates = new java.util.LinkedHashSet<>();
        for (UUID id : waiting.keySet()) {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(id);
            if (player != null) {
                candidates.add(player);
            }
        }
        for (BenchSeatEntity seat : level.getEntitiesOfClass(BenchSeatEntity.class, new AABB(at, at).inflate(3.0))) {
            for (Entity sitter : seat.getPassengers()) {
                if (sitter instanceof ServerPlayer player) {
                    candidates.add(player);
                }
            }
        }
        for (ServerPlayer player : candidates) {
            if (player.position().distanceTo(at) > WAIT_REACH + 2.0 || !waitingAt(player, stop) || bus.hasPassenger(player)) {
                continue;
            }
            if (bus.getPassengers().size() >= bus.carType().seatCount()) {
                player.displayClientMessage(Component.translatable("bus.reikocars.full"), true);
                continue;
            }
            if (player.isPassenger()) {
                player.stopRiding(); // off the bench
            }
            if (player.startRiding(bus, true)) {
                waiting.remove(player.getUUID());
                Component routeName = bus.busRoute() > 0 ? Component.translatable("bus.reikocars.route", bus.busRoute()) : bus.kindName();
                player.displayClientMessage(Component.translatable("bus.reikocars.boarded", routeName), true);
                on++;
            }
        }
        return on;
    }

    /** Villagers by a stop on the bus's route may get on, each for another stop on it. Returns how many did. */
    private static int villagersOn(ServerLevel level, CarEntity bus, BusStopBlockEntity stop, GlobalPos here, List<BusStopRegistry.Stop> route,
            long now) {
        if (!CarsConfig.get(CarsConfig.VILLAGER_PASSENGERS) || route.size() < 2) {
            return 0;
        }
        List<BusStopRegistry.Stop> destinations = new ArrayList<>();
        for (BusStopRegistry.Stop other : route) {
            if (!other.pos().equals(here) && Math.sqrt(other.pos().pos().distSqr(here.pos())) >= MIN_VILLAGER_TRIP) {
                destinations.add(other);
            }
        }
        if (destinations.isEmpty()) {
            return 0;
        }
        Vec3 at = spot(stop);
        List<Villager> near = new ArrayList<>(level.getEntitiesOfClass(Villager.class, new AABB(at, at).inflate(VILLAGER_REACH, 2.0, VILLAGER_REACH),
                villager -> villager.isAlive() && !villager.isBaby() && !villager.isPassenger() && !villager.isSleeping() && !villager.isTrading()
                        && rested(villager, now)));
        Collections.shuffle(near, new java.util.Random(level.getRandom().nextLong()));
        double chance = CarsConfig.get(CarsConfig.VILLAGER_CHANCE);
        int on = 0;
        for (Villager villager : near) {
            if (on >= VILLAGERS_A_STOP || bus.getPassengers().size() >= bus.carType().seatCount()) {
                break;
            }
            if (level.getRandom().nextDouble() >= chance) {
                continue;
            }
            BusStopRegistry.Stop to = destinations.get(level.getRandom().nextInt(destinations.size()));
            if (villager.startRiding(bus, true)) {
                bus.busLine().villagers().put(villager.getUUID(), new BusLine.Ride(here, to.pos(), now));
                villager.getPersistentData().putLong(RODE, now);
                on++;
            }
        }
        return on;
    }

    private static boolean rested(Villager villager, long now) {
        if (!villager.getPersistentData().contains(RODE)) {
            return true;
        }
        long last = villager.getPersistentData().getLong(RODE);
        return now - last >= VILLAGER_REST || now < last;
    }

    /** The bus has stopped at {@code stop} on its route: on to the next, and the driver's paid for a stop served in order. */
    private static void serve(ServerPlayer driver, CarEntity bus, BusStopRegistry.Stop stop, GlobalPos here) {
        BusLine line = bus.busLine();
        GlobalPos last = line.lastServed();
        boolean inOrder = line.nextNumber() > 0 && stop.numberOn(line.route()) == line.nextNumber();
        double leg = last == null || last.dimension() != here.dimension() ? 0.0 : Math.sqrt(horizontalDistanceSqr(last.pos(), here.pos()));
        boolean paid = inOrder && last != null && !last.equals(here) && leg >= MIN_LEG && line.drivenSince() >= leg * 0.8;
        line.served(stop);
        if (paid) {
            CarJobs.work(driver, CarJobs.BUS_STOP, 1);
        }
    }

    private static double horizontalDistanceSqr(BlockPos a, BlockPos b) {
        double dx = a.getX() - b.getX();
        double dz = a.getZ() - b.getZ();
        return dx * dx + dz * dz;
    }

    private static Component stopName(ServerLevel level, GlobalPos pos) {
        BusStopRegistry.Stop stop = BusStopRegistry.get(level.getServer()).find(pos);
        return stop == null || stop.name().isEmpty() ? Component.translatable("bus.reikocars.unnamed_stop") : Component.literal(stop.name());
    }

    /** Whether {@code player} is riding {@code bus} but not driving it. */
    public static boolean isPassenger(Player player, CarEntity bus) {
        return bus.hasPassenger(player) && bus.getControllingPassenger() != player;
    }
}
