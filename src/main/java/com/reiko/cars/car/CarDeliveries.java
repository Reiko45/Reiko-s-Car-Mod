package com.reiko.cars.car;

import com.reiko.cars.ReikoCars;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Cars on their way somewhere: a rental going back to its lot, a repossessed car going to an impound lot. The car is
 * taken out of the world where it is (its riders and any trailer let off first) and saved here; it's put down again,
 * just as it was, at the first free spot near where it's going once that place is loaded. That way a car can be sent
 * to a place nobody's near, and it's never lost or doubled if the server stops in between.
 */
public final class CarDeliveries extends SavedData {
    private static final String NAME = "reikocars_car_deliveries";
    /** How far from the spot it was sent to it may be put down, looking for room. */
    private static final int SEARCH = 8;

    private record Delivery(ResourceKey<Level> dimension, Vec3 target, float yaw, CompoundTag car) {
    }

    private final List<Delivery> pending = new ArrayList<>();

    public static CarDeliveries get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(CarDeliveries::new, CarDeliveries::load), NAME);
    }

    /** Sends {@code car} to {@code target} in {@code dimension}, facing {@code yaw}. It leaves at once; it arrives when there's loaded. */
    public static void send(CarEntity car, ResourceKey<Level> dimension, Vec3 target, float yaw) {
        if (!(car.level() instanceof ServerLevel level) || car.isRemoved()) {
            return;
        }
        car.ejectPassengers();
        car.stopRiding();
        CompoundTag saved = new CompoundTag();
        car.saveWithoutId(saved);
        saved.putString("id", EntityType.getKey(car.getType()).toString());
        saved.remove("Passengers");
        car.remove(Entity.RemovalReason.CHANGED_DIMENSION); // gone from here, its load kept: not dropped, not saved here
        CarDeliveries deliveries = get(level.getServer());
        deliveries.pending.add(new Delivery(dimension, target, yaw, saved));
        deliveries.setDirty();
        deliveries.tick(level.getServer());
    }

    /** How many cars are on their way: for tests. */
    public int pending() {
        return pending.size();
    }

    /** Puts down every car whose destination is loaded. On the server, every second (and when one's sent). */
    public void tick(MinecraftServer server) {
        Iterator<Delivery> it = pending.iterator();
        while (it.hasNext()) {
            Delivery delivery = it.next();
            ServerLevel level = server.getLevel(delivery.dimension());
            if (level == null) {
                ReikoCars.LOGGER.warn("A car on its way to {} has nowhere to go: that dimension is gone", delivery.dimension().location());
                it.remove();
                setDirty();
                continue;
            }
            BlockPos at = BlockPos.containing(delivery.target());
            if (!level.isLoaded(at) || !level.areEntitiesLoaded(ChunkPos.asLong(at))) {
                continue;
            }
            it.remove();
            setDirty();
            Entity entity = EntityType.create(delivery.car(), level).orElse(null);
            if (entity == null) {
                ReikoCars.LOGGER.warn("A car on its way to {} couldn't be put back together", at);
                continue;
            }
            if (level.getEntity(entity.getUUID()) != null) {
                continue; // it's here already (it can't be, but never two of one car)
            }
            Vec3 spot = freeSpot(level, entity, delivery.target(), delivery.yaw());
            entity.moveTo(spot.x, spot.y, spot.z, delivery.yaw(), 0.0F);
            entity.setDeltaMovement(Vec3.ZERO);
            level.addFreshEntity(entity);
        }
    }

    /**
     * The nearest place to {@code target} with room for {@code entity} facing {@code yaw}, on the sturdy top of a block
     * (so never on a desk, a pump or a fence), clear of other cars; {@code target} itself if there's none.
     */
    static Vec3 freeSpot(ServerLevel level, Entity entity, Vec3 target, float yaw) {
        for (int r = 0; r <= SEARCH; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) {
                        continue;
                    }
                    for (int dy : new int[] {0, 1, -1, 2, -2}) {
                        Vec3 at = new Vec3(Math.floor(target.x) + dx + 0.5, Math.floor(target.y) + dy, Math.floor(target.z) + dz + 0.5);
                        BlockPos below = BlockPos.containing(at.x, at.y - 0.5, at.z);
                        if (!level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) {
                            continue;
                        }
                        AABB box = footprint(entity, at, yaw);
                        if (level.noCollision(entity, box) && level.getEntitiesOfClass(CarEntity.class, box.inflate(0.3),
                                other -> other != entity && !other.isPassenger()).stream().noneMatch(other ->
                                footprint(other, other.position(), other.getYRot()).intersects(box))) {
                            return at;
                        }
                    }
                }
            }
        }
        return target;
    }

    /** The ground a car covers at {@code at} facing {@code yaw} (its hitbox is smaller than it looks), as a box. */
    static AABB footprint(Entity entity, Vec3 at, float yaw) {
        if (!(entity instanceof CarEntity car)) {
            return entity.getType().getDimensions().makeBoundingBox(at);
        }
        CarType type = car.carType();
        double radians = Math.toRadians(yaw);
        double sin = Math.abs(Math.sin(radians));
        double cos = Math.abs(Math.cos(radians));
        double halfX = (cos * type.width + sin * type.length) / 2.0;
        double halfZ = (sin * type.width + cos * type.length) / 2.0;
        return new AABB(at.x - halfX, at.y + 0.05, at.z - halfZ, at.x + halfX, at.y + Math.max(1.0, type.height), at.z + halfZ);
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Delivery delivery : pending) {
            CompoundTag entry = new CompoundTag();
            entry.putString("Dimension", delivery.dimension().location().toString());
            entry.putDouble("X", delivery.target().x);
            entry.putDouble("Y", delivery.target().y);
            entry.putDouble("Z", delivery.target().z);
            entry.putFloat("Yaw", delivery.yaw());
            entry.put("Car", delivery.car());
            list.add(entry);
        }
        tag.put("Deliveries", list);
        return tag;
    }

    private static CarDeliveries load(CompoundTag tag, HolderLookup.Provider registries) {
        CarDeliveries deliveries = new CarDeliveries();
        for (Tag t : tag.getList("Deliveries", Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) t;
            ResourceLocation dimension = ResourceLocation.tryParse(entry.getString("Dimension"));
            if (dimension != null) {
                deliveries.pending.add(new Delivery(ResourceKey.create(Registries.DIMENSION, dimension),
                        new Vec3(entry.getDouble("X"), entry.getDouble("Y"), entry.getDouble("Z")), entry.getFloat("Yaw"),
                        entry.getCompound("Car")));
            }
        }
        return deliveries;
    }
}
