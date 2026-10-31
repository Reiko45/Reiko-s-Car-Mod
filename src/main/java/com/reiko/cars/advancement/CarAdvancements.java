package com.reiko.cars.advancement;

import com.reiko.cars.ReikoCars;
import com.reiko.cars.car.CarEntity;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The mod's advancements (in {@code data/reikocars/advancement}, written by {@code tools/advancements}): what happens on
 * the road, told to the {@link CarTrigger} as named events. Server side.
 *
 * <p>The events: {@code drive} (behind the wheel), {@code trip} (blocks driven without getting out), {@code fuel},
 * {@code charge}, {@code out_of_fuel}, {@code buy_car}, {@code fleet} (cars owned), {@code license}, {@code plate},
 * {@code camera}, {@code crash}, {@code repair}, {@code wash}, {@code hitch}, {@code semi}, {@code camper}, {@code tune},
 * {@code race_win}, {@code pave} (blocks of road laid in one go), {@code garage}, {@code job} (any car job), and
 * {@code job_} plus the job's action ({@code job_taxi_ride}, {@code job_bus_stop}, {@code job_parcel_delivered},
 * {@code job_tow}...).
 */
@EventBusSubscriber(modid = ReikoCars.MODID)
public final class CarAdvancements {
    public static final DeferredRegister<CriterionTrigger<?>> TRIGGERS = DeferredRegister.create(Registries.TRIGGER_TYPE, ReikoCars.MODID);
    public static final DeferredHolder<CriterionTrigger<?>, CarTrigger> CAR = TRIGGERS.register("car", CarTrigger::new);

    /** How often a trip's length is reported, in blocks. */
    static final double TRIP_STEP = 50.0;
    /** A gap this long (ticks) between one tick of driving and the next is a new trip: they got out. */
    public static final int TRIP_BREAK = 20;

    /** Each driver's trip so far: the car, how far, when they last drove, and how far had been reported. */
    private static final Map<UUID, Trip> TRIPS = new HashMap<>();

    private CarAdvancements() {
    }

    /** {@code player} did {@code event}. */
    public static void award(@Nullable ServerPlayer player, String event) {
        reach(player, event, 1.0);
    }

    /** {@code player} has got to {@code value} of {@code event} (blocks driven, cars owned...). */
    public static void reach(@Nullable ServerPlayer player, String event, double value) {
        if (player != null && !player.isRemoved()) {
            CAR.get().trigger(player, event, value);
        }
    }

    /** {@code player} did one of the car jobs' {@code action}s (a taxi ride, a bus stop, a parcel delivered...). */
    public static void job(@Nullable ServerPlayer player, ResourceLocation action) {
        award(player, "job");
        award(player, "job_" + action.getPath());
    }

    /** {@code player}'s driving {@code car}: {@code moved} blocks this tick. Counts the trip, from getting in to getting out. */
    public static void drove(ServerPlayer player, CarEntity car, double moved) {
        long now = player.server.getTickCount();
        Trip trip = TRIPS.get(player.getUUID());
        if (trip == null || !trip.car.equals(car.getUUID()) || now - trip.last > TRIP_BREAK) {
            trip = new Trip(car.getUUID());
            TRIPS.put(player.getUUID(), trip);
            award(player, "drive");
        }
        trip.last = now;
        trip.blocks += moved;
        if (trip.blocks >= trip.reported + TRIP_STEP) {
            trip.reported = Math.floor(trip.blocks / TRIP_STEP) * TRIP_STEP;
            reach(player, "trip", trip.blocks);
        }
    }

    /** Logging in: the cars they own already count (from before there were advancements, say). */
    @SubscribeEvent
    static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            reach(player, "fleet", com.reiko.cars.gps.CarLocator.get(player.server).ownedBy(player.getUUID()));
        }
    }

    /** Logging out ends a trip. */
    @SubscribeEvent
    static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        TRIPS.remove(event.getEntity().getUUID());
    }

    /** The blocks {@code player}'s driven this trip, or 0. */
    public static double tripBlocks(UUID player) {
        Trip trip = TRIPS.get(player);
        return trip == null ? 0.0 : trip.blocks;
    }

    private static final class Trip {
        final UUID car;
        long last;
        double blocks;
        double reported;

        Trip(UUID car) {
            this.car = car;
        }
    }
}
