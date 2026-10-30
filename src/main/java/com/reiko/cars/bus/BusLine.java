package com.reiko.cars.bus;

import com.reiko.cars.car.CarEntity;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

/**
 * A city bus's route, on the server: the route its owner gave it, the stop it's heading for next, the passengers who've
 * rung for the next stop, and the villagers riding it and where they're going. The route's number, the bell and the
 * next stop's name are synced to clients for the dashboard.
 */
public final class BusLine {
    /** How often the next stop's name is looked up again (a stop may have been renamed or moved), in ticks. */
    private static final int REFRESH = 40;

    /** A villager's ride: where they got on and where they're getting off, and when they got on. */
    public record Ride(GlobalPos from, GlobalPos to, long boardedAt) {
    }

    private final CarEntity bus;
    private int route;
    /** The number of the stop it's heading for along its route; 0 before it's served one. */
    private int nextNumber;
    @Nullable
    private GlobalPos lastServed;
    /** Blocks driven since it last served a stop on its route. */
    private double drivenSince;
    private final Set<UUID> ringers = new HashSet<>();
    private final Map<UUID, Ride> villagers = new HashMap<>();
    private int ticks;

    public BusLine(CarEntity bus) {
        this.bus = bus;
    }

    public int route() {
        return route;
    }

    /** Its owner gives it a route (0 for none): it starts again from the route's first stop. */
    public void setRoute(int route) {
        if (route != this.route) {
            this.route = route;
            nextNumber = 0;
            lastServed = null;
            drivenSince = 0.0;
            sync();
        }
    }

    public int nextNumber() {
        return nextNumber;
    }

    @Nullable
    public GlobalPos lastServed() {
        return lastServed;
    }

    public double drivenSince() {
        return drivenSince;
    }

    /** Its route's stops, in order (its owner's stops on that route, in its dimension). */
    public List<BusStopRegistry.Stop> stops() {
        if (route <= 0 || !(bus.level() instanceof ServerLevel level)) {
            return List.of();
        }
        return BusStopRegistry.get(level.getServer()).route(bus.owner(), route, level.dimension());
    }

    /** The stop it's heading for: the one numbered {@link #nextNumber()}, or the next after; the first to begin with. */
    @Nullable
    public BusStopRegistry.Stop nextStop() {
        List<BusStopRegistry.Stop> stops = stops();
        if (stops.isEmpty()) {
            return null;
        }
        for (BusStopRegistry.Stop stop : stops) {
            if (stop.numberOn(route) >= nextNumber) {
                return stop;
            }
        }
        return stops.getFirst();
    }

    /** It's stopped at {@code stop} on its route: it heads for the one after (back to the first after the last). */
    void served(BusStopRegistry.Stop stop) {
        List<BusStopRegistry.Stop> stops = stops();
        int at = stops.indexOf(stop);
        BusStopRegistry.Stop next = stops.isEmpty() ? null : stops.get(at < 0 ? 0 : (at + 1) % stops.size());
        nextNumber = next == null ? 0 : next.numberOn(route);
        lastServed = stop.pos();
        drivenSince = 0.0;
        sync();
    }

    // ---- the bell ----

    public boolean rung(UUID rider) {
        return ringers.contains(rider);
    }

    /** Returns false if they'd rung already. */
    boolean ring(UUID rider) {
        boolean added = ringers.add(rider);
        sync();
        return added;
    }

    Set<UUID> ringers() {
        return ringers;
    }

    void clearBell() {
        if (!ringers.isEmpty()) {
            ringers.clear();
            sync();
        }
    }

    // ---- villagers ----

    public Map<UUID, Ride> villagers() {
        return villagers;
    }

    // ---- ticking ----

    /** A server tick, {@code moved} blocks on from the last. */
    public void tick(double moved) {
        drivenSince += moved;
        if (++ticks % REFRESH == 0) {
            sync();
        }
    }

    /** Someone got off (or out). */
    public void riderLeft(Entity passenger) {
        if (ringers.remove(passenger.getUUID())) {
            sync();
        }
        villagers.remove(passenger.getUUID());
    }

    /** Tells clients the route, the bell and the next stop's name. */
    void sync() {
        BusStopRegistry.Stop next = route > 0 ? nextStop() : null;
        String name = next == null ? "" : next.name().isEmpty() ? "?" : next.name();
        bus.setBusDisplay(route, !ringers.isEmpty(), name);
    }

    // ---- saving ----

    public void save(CompoundTag tag) {
        if (route == 0 && villagers.isEmpty()) {
            return;
        }
        CompoundTag line = new CompoundTag();
        line.putInt("Route", route);
        line.putInt("Next", nextNumber);
        if (lastServed != null) {
            GlobalPos.CODEC.encodeStart(NbtOps.INSTANCE, lastServed).result().ifPresent(pos -> line.put("LastServed", pos));
        }
        line.putDouble("Driven", drivenSince);
        ListTag rides = new ListTag();
        villagers.forEach((id, ride) -> {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("Villager", id);
            GlobalPos.CODEC.encodeStart(NbtOps.INSTANCE, ride.from()).result().ifPresent(pos -> entry.put("From", pos));
            GlobalPos.CODEC.encodeStart(NbtOps.INSTANCE, ride.to()).result().ifPresent(pos -> entry.put("To", pos));
            entry.putLong("At", ride.boardedAt());
            rides.add(entry);
        });
        line.put("Villagers", rides);
        tag.put("BusLine", line);
    }

    public void load(CompoundTag tag) {
        if (!tag.contains("BusLine")) {
            return;
        }
        CompoundTag line = tag.getCompound("BusLine");
        route = line.getInt("Route");
        nextNumber = line.getInt("Next");
        lastServed = line.contains("LastServed") ? GlobalPos.CODEC.parse(NbtOps.INSTANCE, line.get("LastServed")).result().orElse(null) : null;
        drivenSince = line.getDouble("Driven");
        villagers.clear();
        for (Tag t : line.getList("Villagers", Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) t;
            GlobalPos from = GlobalPos.CODEC.parse(NbtOps.INSTANCE, entry.get("From")).result().orElse(null);
            GlobalPos to = GlobalPos.CODEC.parse(NbtOps.INSTANCE, entry.get("To")).result().orElse(null);
            if (entry.hasUUID("Villager") && from != null && to != null) {
                villagers.put(entry.getUUID("Villager"), new Ride(from, to, entry.getLong("At")));
            }
        }
    }
}
