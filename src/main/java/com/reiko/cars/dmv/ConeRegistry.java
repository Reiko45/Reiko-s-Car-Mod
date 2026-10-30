package com.reiko.cars.dmv;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Every numbered traffic cone: where, whose and its number. A DMV's test route is its owner's numbered cones near it,
 * in number order, and they're found here even when their chunks aren't loaded.
 */
public final class ConeRegistry extends SavedData {
    private static final String NAME = "reikocars_test_cones";

    public record Cone(GlobalPos pos, UUID owner, int number) {
    }

    private final List<Cone> cones = new ArrayList<>();

    public static ConeRegistry get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(ConeRegistry::new, ConeRegistry::load), NAME);
    }

    /** Records the cone at {@code pos}: numbered {@code number}, or not on any route if 0. */
    public void set(GlobalPos pos, UUID owner, int number) {
        boolean changed = cones.removeIf(cone -> cone.pos().equals(pos));
        if (number > 0) {
            cones.add(new Cone(pos, owner, number));
            changed = true;
        }
        if (changed) {
            setDirty();
        }
    }

    public void remove(GlobalPos pos) {
        if (cones.removeIf(cone -> cone.pos().equals(pos))) {
            setDirty();
        }
    }

    /** {@code owner}'s numbered cones within {@code radius} of {@code center} in {@code dimension}, in number order. */
    public List<BlockPos> route(ResourceKey<Level> dimension, BlockPos center, double radius, UUID owner) {
        return cones.stream()
                .filter(cone -> cone.pos().dimension() == dimension && owner.equals(cone.owner())
                        && cone.pos().pos().distSqr(center) <= radius * radius)
                .sorted(Comparator.comparingInt(Cone::number).thenComparing(cone -> cone.pos().pos()))
                .map(cone -> cone.pos().pos())
                .toList();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Cone cone : cones) {
            CompoundTag entry = new CompoundTag();
            GlobalPos.CODEC.encodeStart(NbtOps.INSTANCE, cone.pos()).result().ifPresent(saved -> entry.put("Pos", saved));
            entry.putUUID("Owner", cone.owner());
            entry.putInt("Number", cone.number());
            list.add(entry);
        }
        tag.put("Cones", list);
        return tag;
    }

    private static ConeRegistry load(CompoundTag tag, HolderLookup.Provider registries) {
        ConeRegistry registry = new ConeRegistry();
        for (Tag t : tag.getList("Cones", Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) t;
            if (entry.hasUUID("Owner")) {
                GlobalPos.CODEC.parse(NbtOps.INSTANCE, entry.get("Pos")).result()
                        .ifPresent(pos -> registry.cones.add(new Cone(pos, entry.getUUID("Owner"), entry.getInt("Number"))));
            }
        }
        return registry;
    }
}
