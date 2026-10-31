package com.reiko.cars.gps;

import com.reiko.cars.charge.ChargingStationBlockEntity;
import com.reiko.cars.dmv.DmvOfficeBlockEntity;
import com.reiko.cars.impound.ImpoundOfficeBlockEntity;
import com.reiko.cars.insurance.InsuranceOfficeBlockEntity;
import com.reiko.cars.parking.ParkingMeterBlockEntity;
import com.reiko.cars.pump.GasPumpBlockEntity;
import com.reiko.cars.racing.RaceControlBlockEntity;
import com.reiko.cars.rental.RentalDeskBlockEntity;
import com.reiko.cars.repair.RepairBenchBlockEntity;
import com.reiko.cars.sale.DealershipBlockEntity;
import com.reiko.cars.tuning.TuningBenchBlockEntity;
import com.reiko.cars.wash.CarWashBlockEntity;
import com.reiko.cars.wash.WashBayBlockEntity;
import com.reiko.cars.wash.WashBusinessBlockEntity;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

/**
 * Every place the GPS knows of, in every dimension, even where the chunks aren't loaded: the gas pumps, chargers,
 * mechanics, car washes and offices. A chunk's places are read from it each time it loads (and again when a block's
 * placed or broken in it, or the GPS is opened near it), so the list catches up with whatever's been built or knocked
 * down.
 */
public final class PlacesRegistry extends SavedData {
    private static final String NAME = "reikocars_gps_places";

    /**
     * One place.
     *
     * @param variant gasoline or diesel for a pump (the fuel's ordinal); 0 for a drive-through car wash, 1 for a wash bay;
     *                0 for a parking meter, 1 for a parking garage
     * @param price   a pump's price a bucket, a charger's a 1,000 MJ, a car wash's a wash, a parking garage's an hour, in
     *                cents; -1 for none
     * @param open    whether it can serve anyone: a pump with fuel in it, a car wash with an owner
     * @param extra   a bus stop's routes ("1, 4"); "" for anything else
     */
    public record Place(BlockPos pos, PlaceKind kind, String owner, int variant, long price, boolean open, String extra) {
        public Place(BlockPos pos, PlaceKind kind, String owner, int variant, long price, boolean open) {
            this(pos, kind, owner, variant, price, open, "");
        }
    }

    private final Map<ResourceKey<Level>, Long2ObjectMap<List<Place>>> places = new HashMap<>();

    public static PlacesRegistry get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(PlacesRegistry::new, PlacesRegistry::load), NAME);
    }

    /** What {@code entity} makes of a place, or null if it isn't one. */
    @Nullable
    public static Place describe(BlockEntity entity) {
        PlaceKind kind = PlaceKind.of(entity);
        if (kind == null) {
            return null;
        }
        BlockPos pos = entity.getBlockPos().immutable();
        return switch (entity) {
            case GasPumpBlockEntity pump -> new Place(pos, kind, pump.ownerName(), pump.fuelType().ordinal(), pump.price(), !pump.fuel().isEmpty());
            case ChargingStationBlockEntity charger -> new Place(pos, kind, charger.ownerName(), 0, charger.price(), true);
            case WashBusinessBlockEntity wash -> new Place(pos, kind, wash.ownerName(), wash instanceof WashBayBlockEntity ? 1 : 0, wash.price(),
                    wash.owner() != null);
            case RepairBenchBlockEntity bench -> new Place(pos, kind, bench.ownerName(), 0, -1, true);
            case DealershipBlockEntity lot -> new Place(pos, kind, lot.ownerName(), 0, -1, true);
            case RentalDeskBlockEntity desk -> new Place(pos, kind, desk.ownerName(), 0, -1, true);
            case TuningBenchBlockEntity bench -> new Place(pos, kind, bench.ownerName(), 0, -1, true);
            case ParkingMeterBlockEntity meter -> new Place(pos, kind, meter.ownerName(), 0, -1, true);
            // (a parking garage goes by its name, or its owner's if it hasn't one; its price is an hour's)
            case com.reiko.cars.garage.GarageDoorBlockEntity door -> new Place(pos, kind,
                    door.settings().name().isEmpty() ? door.ownerName() : door.settings().name(), 1, door.settings().hourPrice(), true);
            case DmvOfficeBlockEntity office -> new Place(pos, kind, office.ownerName(), 0, -1, true);
            case InsuranceOfficeBlockEntity office -> new Place(pos, kind, office.ownerName(), 0, -1, true);
            case ImpoundOfficeBlockEntity office -> new Place(pos, kind, office.ownerName(), 0, -1, true);
            case RaceControlBlockEntity race -> new Place(pos, kind, race.ownerName(), 0, -1, true);
            // (a bus stop goes by its name, or its owner's if it hasn't one)
            case com.reiko.cars.bus.BusStopBlockEntity stop -> new Place(pos, kind, stop.name().isEmpty() ? stop.ownerName() : stop.name(), 0, -1, true,
                    stop.routesText());
            default -> null;
        };
    }

    /** Reads the places in a loaded chunk, replacing what was known of it. */
    public void scan(LevelChunk chunk) {
        List<Place> found = new ArrayList<>();
        for (BlockEntity entity : chunk.getBlockEntities().values()) {
            if (!entity.isRemoved()) {
                Place place = describe(entity);
                if (place != null) {
                    found.add(place);
                }
            }
        }
        found.sort(Comparator.comparingLong(place -> place.pos().asLong()));
        ResourceKey<Level> dimension = chunk.getLevel().dimension();
        long key = chunk.getPos().toLong();
        Long2ObjectMap<List<Place>> known = places.get(dimension);
        List<Place> before = known == null ? null : known.get(key);
        if (found.isEmpty()) {
            if (before != null) {
                known.remove(key);
                setDirty();
            }
        } else if (!found.equals(before)) {
            places.computeIfAbsent(dimension, d -> new Long2ObjectOpenHashMap<>()).put(key, List.copyOf(found));
            setDirty();
        }
    }

    /** Every place known in {@code dimension}. */
    public List<Place> in(ResourceKey<Level> dimension) {
        Long2ObjectMap<List<Place>> known = places.get(dimension);
        List<Place> all = new ArrayList<>();
        if (known != null) {
            known.values().forEach(all::addAll);
        }
        return all;
    }

    /** The chunks in {@code dimension} with places in them. */
    public List<ChunkPos> chunks(ResourceKey<Level> dimension) {
        Long2ObjectMap<List<Place>> known = places.get(dimension);
        return known == null ? List.of() : known.keySet().longStream().mapToObj(ChunkPos::new).toList();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        places.forEach((dimension, chunks) -> chunks.values().forEach(inChunk -> {
            for (Place place : inChunk) {
                CompoundTag entry = new CompoundTag();
                entry.putString("Dim", dimension.location().toString());
                entry.putLong("Pos", place.pos().asLong());
                entry.putString("Kind", place.kind().id());
                entry.putString("Owner", place.owner());
                entry.putInt("Variant", place.variant());
                entry.putLong("Price", place.price());
                entry.putBoolean("Open", place.open());
                entry.putString("Extra", place.extra());
                list.add(entry);
            }
        }));
        tag.put("Places", list);
        return tag;
    }

    private static PlacesRegistry load(CompoundTag tag, HolderLookup.Provider registries) {
        PlacesRegistry registry = new PlacesRegistry();
        for (Tag t : tag.getList("Places", Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) t;
            ResourceLocation dim = ResourceLocation.tryParse(entry.getString("Dim"));
            PlaceKind kind = kind(entry.getString("Kind"));
            if (dim == null || kind == null) {
                continue;
            }
            BlockPos pos = BlockPos.of(entry.getLong("Pos"));
            Place place = new Place(pos, kind, entry.getString("Owner"), entry.getInt("Variant"), entry.getLong("Price"), entry.getBoolean("Open"),
                    entry.getString("Extra"));
            registry.places.computeIfAbsent(ResourceKey.create(Registries.DIMENSION, dim), d -> new Long2ObjectOpenHashMap<>())
                    .computeIfAbsent(ChunkPos.asLong(pos), k -> new ArrayList<>()).add(place);
        }
        // In the same order a scan finds them, so a chunk that hasn't changed isn't taken for one that has.
        registry.places.values().forEach(chunks -> chunks.replaceAll((key, inChunk) -> {
            List<Place> sorted = new ArrayList<>(inChunk);
            sorted.sort(Comparator.comparingLong(place -> place.pos().asLong()));
            return List.copyOf(sorted);
        }));
        return registry;
    }

    @Nullable
    private static PlaceKind kind(String id) {
        for (PlaceKind kind : PlaceKind.values()) {
            if (kind.id().equals(id)) {
                return kind;
            }
        }
        return null;
    }
}
