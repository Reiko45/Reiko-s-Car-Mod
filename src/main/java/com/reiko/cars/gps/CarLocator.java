package com.reiko.cars.gps;

import com.reiko.cars.car.CarEntity;
import com.reiko.cars.car.CarType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

/**
 * Where every owned car was last seen, so the GPS can take its owner (or whoever's renting it) back to it, however far
 * away it's parked. A car reports in every couple of seconds; one that's wrecked, picked up, sold or towed off to a lot
 * is forgotten, and one that's just unloaded stays where it was.
 */
public final class CarLocator extends SavedData {
    private static final String NAME = "reikocars_gps_cars";
    /** How often a car reports where it is, in ticks. */
    static final int EVERY = 40;

    /** Where a car is, and what to call it. */
    public record Spot(UUID car, UUID owner, @Nullable UUID renter, GlobalPos pos, CarType type, int color, String plate, String name) {
        boolean sameAs(Spot other) {
            return owner.equals(other.owner) && Objects.equals(renter, other.renter) && pos.equals(other.pos) && type == other.type
                    && color == other.color && plate.equals(other.plate) && name.equals(other.name);
        }
    }

    private final Map<UUID, Spot> spots = new HashMap<>();

    public static CarLocator get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(CarLocator::new, CarLocator::load), NAME);
    }

    /** A car's server tick: every {@link #EVERY} ticks, notes where it is. */
    public static void carTick(CarEntity car) {
        if ((car.tickCount + car.getId()) % EVERY == 0 && car.level() instanceof ServerLevel level) {
            get(level.getServer()).note(car);
        }
    }

    /** A car's leaving the world: forgotten unless it's only being unloaded (or changing dimension). */
    public static void carRemoved(CarEntity car, Entity.RemovalReason reason) {
        if (reason.shouldDestroy() && car.level() instanceof ServerLevel level) {
            get(level.getServer()).forget(car.getUUID());
        }
    }

    /** Notes where {@code car} is now, if it has an owner. */
    public void note(CarEntity car) {
        UUID owner = car.owner();
        if (owner == null) {
            forget(car.getUUID());
            return;
        }
        String name = car.hasCustomName() ? car.getCustomName().getString() : "";
        Spot spot = new Spot(car.getUUID(), owner, car.renter(), GlobalPos.of(car.level().dimension(), car.blockPosition()), car.carType(),
                car.color(), car.plate(), name);
        Spot before = spots.get(spot.car());
        if (before == null || !before.sameAs(spot)) {
            spots.put(spot.car(), spot);
            setDirty();
        }
    }

    public void forget(UUID car) {
        if (spots.remove(car) != null) {
            setDirty();
        }
    }

    @Nullable
    public Spot find(UUID car) {
        return spots.get(car);
    }

    /** The cars {@code player} owns or is renting. */
    public List<Spot> carsOf(UUID player) {
        List<Spot> cars = new ArrayList<>();
        for (Spot spot : spots.values()) {
            if (player.equals(spot.owner()) || player.equals(spot.renter())) {
                cars.add(spot);
            }
        }
        return cars;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Spot spot : spots.values()) {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("Car", spot.car());
            entry.putUUID("Owner", spot.owner());
            if (spot.renter() != null) {
                entry.putUUID("Renter", spot.renter());
            }
            GlobalPos.CODEC.encodeStart(NbtOps.INSTANCE, spot.pos()).result().ifPresent(pos -> entry.put("Pos", pos));
            entry.putString("Type", spot.type().id());
            entry.putInt("Color", spot.color());
            entry.putString("Plate", spot.plate());
            entry.putString("Name", spot.name());
            list.add(entry);
        }
        tag.put("Cars", list);
        return tag;
    }

    private static CarLocator load(CompoundTag tag, HolderLookup.Provider registries) {
        CarLocator locator = new CarLocator();
        for (Tag t : tag.getList("Cars", Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) t;
            CarType type = type(entry.getString("Type"));
            if (!entry.hasUUID("Car") || !entry.hasUUID("Owner") || type == null) {
                continue;
            }
            UUID renter = entry.hasUUID("Renter") ? entry.getUUID("Renter") : null;
            GlobalPos.CODEC.parse(NbtOps.INSTANCE, entry.get("Pos")).result().ifPresent(pos -> {
                Spot spot = new Spot(entry.getUUID("Car"), entry.getUUID("Owner"), renter, pos, type, entry.getInt("Color"),
                        entry.getString("Plate"), entry.getString("Name"));
                locator.spots.put(spot.car(), spot);
            });
        }
        return locator;
    }

    @Nullable
    private static CarType type(String id) {
        for (CarType type : CarType.values()) {
            if (type.id().equals(id)) {
                return type;
            }
        }
        return null;
    }
}
