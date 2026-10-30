package com.reiko.cars.bus;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

/**
 * Every bus stop, with its owner, name and routes, so a bus knows its route's stops (and their names) even where their
 * chunks aren't loaded.
 */
public final class BusStopRegistry extends SavedData {
    private static final String NAME = "reikocars_bus_stops";

    public record Stop(GlobalPos pos, @Nullable UUID owner, String name, List<BusStopBlockEntity.RouteStop> routes) {
        public int numberOn(int route) {
            for (BusStopBlockEntity.RouteStop stop : routes) {
                if (stop.route() == route) {
                    return stop.number();
                }
            }
            return 0;
        }
    }

    private final Map<GlobalPos, Stop> stops = new HashMap<>();

    public static BusStopRegistry get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(BusStopRegistry::new, BusStopRegistry::load), NAME);
    }

    /** Records (or updates) the stop {@code entity}. */
    public void update(BusStopBlockEntity entity) {
        if (entity.getLevel() == null) {
            return;
        }
        Stop stop = new Stop(GlobalPos.of(entity.getLevel().dimension(), entity.getBlockPos().immutable()), entity.owner(), entity.name(),
                entity.routes());
        if (!stop.equals(stops.get(stop.pos()))) {
            stops.put(stop.pos(), stop);
            setDirty();
        }
    }

    public void remove(GlobalPos pos) {
        if (stops.remove(pos) != null) {
            setDirty();
        }
    }

    @Nullable
    public Stop find(GlobalPos pos) {
        return stops.get(pos);
    }

    /** {@code owner}'s stops on {@code route} in {@code dimension}, in number order. */
    public List<Stop> route(@Nullable UUID owner, int route, ResourceKey<Level> dimension) {
        if (owner == null || route <= 0) {
            return List.of();
        }
        return stops.values().stream()
                .filter(stop -> stop.pos().dimension() == dimension && Objects.equals(owner, stop.owner()) && stop.numberOn(route) > 0)
                .sorted(Comparator.comparingInt((Stop stop) -> stop.numberOn(route)).thenComparingLong(stop -> stop.pos().pos().asLong()))
                .toList();
    }

    /** "1, 4": the routes, in order. */
    public static String routesText(List<BusStopBlockEntity.RouteStop> routes) {
        return routes.stream().map(BusStopBlockEntity.RouteStop::route).sorted().distinct().map(String::valueOf).collect(Collectors.joining(", "));
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Stop stop : stops.values()) {
            CompoundTag entry = new CompoundTag();
            GlobalPos.CODEC.encodeStart(NbtOps.INSTANCE, stop.pos()).result().ifPresent(pos -> entry.put("Pos", pos));
            if (stop.owner() != null) {
                entry.putUUID("Owner", stop.owner());
            }
            entry.putString("Name", stop.name());
            int[] packed = new int[stop.routes().size() * 2];
            for (int i = 0; i < stop.routes().size(); i++) {
                packed[i * 2] = stop.routes().get(i).route();
                packed[i * 2 + 1] = stop.routes().get(i).number();
            }
            entry.put("Routes", new IntArrayTag(packed));
            list.add(entry);
        }
        tag.put("Stops", list);
        return tag;
    }

    private static BusStopRegistry load(CompoundTag tag, HolderLookup.Provider registries) {
        BusStopRegistry registry = new BusStopRegistry();
        for (Tag t : tag.getList("Stops", Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) t;
            UUID owner = entry.hasUUID("Owner") ? entry.getUUID("Owner") : null;
            int[] packed = entry.getIntArray("Routes");
            List<BusStopBlockEntity.RouteStop> routes = new ArrayList<>();
            for (int i = 0; i + 1 < packed.length; i += 2) {
                routes.add(new BusStopBlockEntity.RouteStop(packed[i], packed[i + 1]));
            }
            GlobalPos.CODEC.parse(NbtOps.INSTANCE, entry.get("Pos")).result()
                    .ifPresent(pos -> registry.stops.put(pos, new Stop(pos, owner, entry.getString("Name"), List.copyOf(routes))));
        }
        return registry;
    }
}
