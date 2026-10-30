package com.reiko.cars.job;

import com.reiko.cars.ReikoCars;
import com.reiko.economy.api.JobsApi;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

/**
 * The car work the economy mod's job board pays for. Each action is reported once the work is really done (a paid
 * taxi ride, a parcel delivered, a part fitted to someone else's car), and the job files in
 * {@code data/reikocars/jobs} (Driver, Firefighter, Garbage Collector, Mail Carrier, Mechanic, Trucker) say what it pays.
 */
public final class CarJobs {
    /** A taxi ride the rider paid for. */
    public static final ResourceLocation TAXI_RIDE = ReikoCars.id("taxi_ride");
    /** A bus passenger who paid the fare. */
    public static final ResourceLocation BUS_RIDER = ReikoCars.id("bus_rider");
    /** A parcel delivered by mail van. */
    public static final ResourceLocation PARCEL_DELIVERED = ReikoCars.id("parcel_delivered");
    /** A part (tire, brake pad, engine part, body panel, oil) fitted to someone else's vehicle at a repair bench. */
    public static final ResourceLocation REPAIR_PART = ReikoCars.id("repair_part");
    /** Someone else's broken-down car towed in to a repair bench. */
    public static final ResourceLocation TOW = ReikoCars.id("tow");
    /** A bucket of fuel unloaded from a tanker into a pump. */
    public static final ResourceLocation FUEL_DELIVERED = ReikoCars.id("fuel_delivered");
    /** A trash can emptied by the garbage truck (each can pays once a Minecraft day). */
    public static final ResourceLocation TRASH_COLLECTED = ReikoCars.id("trash_collected");
    /** A badly parked car towed to an impound lot. */
    public static final ResourceLocation IMPOUND_TOW = ReikoCars.id("impound_tow");
    /** A fire put out with the fire hose. */
    public static final ResourceLocation FIRE_PUT_OUT = ReikoCars.id("fire_put_out");
    /** 100 blocks driven towing a loaded trailer. */
    public static final ResourceLocation HAUL_DISTANCE = ReikoCars.id("haul_distance");

    /** How far a broken-down car has to be towed for the tow to pay. */
    public static final double MIN_TOW_BLOCKS = 24.0;
    /** How full a trailer has to be to count as loaded. */
    public static final double LOADED = 0.25;

    private CarJobs() {
    }

    /** {@code player} did {@code amount} of {@code action}. Nothing for nobody, or for nothing. */
    public static void work(@Nullable ServerPlayer player, ResourceLocation action, int amount) {
        if (player != null && amount > 0) {
            JobsApi.work(player, action, amount);
        }
    }

    /** Whether any job pays for {@code action}, so there's no point tracking it otherwise. */
    public static boolean paid(ResourceLocation action) {
        return JobsApi.paysFor(action);
    }
}
